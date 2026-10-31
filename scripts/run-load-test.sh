#!/usr/bin/env bash
# Нагрузочный тест: прогон A → профиль → план B → прогон B → профиль → сравнение A↔B.
# Отличается от run-experiment.sh тем, что не использует план C и аналитический
# эталон: здесь проверяется, воспроизводит ли система свой собственный профиль
# при заданном темпе, а не сравнивается с фиксированным эталоном.
#
#   RATE=24 scripts/run-load-test.sh
set -euo pipefail

cd "$(dirname "$0")/.."

RATE="${RATE:-12}"
DURATION="${DURATION:-300}"
SEED="${SEED:-31}"
SETTLE=30
DB_URL="${DB_URL:-jdbc:postgresql://localhost:15432/profile}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

find_java() {
  local candidate
  for candidate in "${JAVA_HOME:-}" "$HOME/.jdks/temurin-21.0.12.1"; do
    if [[ -n $candidate && -x $candidate/bin/java ]] \
        && "$candidate/bin/java" -version 2>&1 | grep -q 'version "2[1-9]'; then
      echo "$candidate"
      return
    fi
  done
  echo "Нужен JDK 21" >&2
  exit 1
}
export JAVA_HOME=$(find_java)
JAVA="$JAVA_HOME/bin/java"

if [[ -x tools/apache-jmeter-5.6.3/bin/jmeter.sh ]]; then
  JMETER=$(pwd)/tools/apache-jmeter-5.6.3/bin/jmeter.sh
elif [[ -n "${JMETER_HOME:-}" && -x "$JMETER_HOME/bin/jmeter.sh" ]]; then
  JMETER="$JMETER_HOME/bin/jmeter.sh"
else
  echo "JMeter не найден" >&2
  exit 1
fi

PROFILER=profiler/target/profiler.jar
GENERATOR=demo-stand/traffic-generator/target/traffic-generator.jar
[[ -f $PROFILER && -f $GENERATOR ]] || { echo "Сначала ./mvnw verify" >&2; exit 1; }

iso() { date -u +%Y-%m-%dT%H:%M:%SZ; }

build_profile() {
  local out
  out=$("$JAVA" -jar "$PROFILER" build-profile \
    --from "$2" --to "$3" --name "$1" \
    --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD")
  echo "$out"
  echo "$out" | grep -ao 'id=[0-9]*' | head -1 | cut -d= -f2
}

OUTDIR="results/load-${RATE}spm"
mkdir -p "$OUTDIR"

docker compose up --build --detach --wait
trap 'kill $MON_PID 2>/dev/null; docker compose down' EXIT
scripts/monitor-stats.sh "$OUTDIR/docker-stats.csv" 5 &
MON_PID=$!

echo "== Прогон A: $RATE spm, $DURATION с, зерно $SEED"
A_FROM=$(iso)
"$JAVA" -jar "$GENERATOR" --sessions-per-minute "$RATE" --duration-seconds "$DURATION" --seed "$SEED" >/dev/null
sleep "$SETTLE"
A_TO=$(iso)
ID_A=$(build_profile "load-${RATE}-a" "$A_FROM" "$A_TO" | tail -1)
[[ -n "$ID_A" ]] || { echo "Профиль A не сохранился" >&2; exit 1; }
echo "Профиль A: id=$ID_A"

"$JAVA" -jar "$PROFILER" generate --profile-id "$ID_A" \
  --host localhost --port 18080 --out "$OUTDIR/plan-b" \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD" >/dev/null

echo "== Прогон B: сгенерированный план"
docker compose restart gateway catalog-service order-service >/dev/null
docker compose up --detach --wait >/dev/null
B_FROM=$(iso)
(cd "$OUTDIR/plan-b" && "$JMETER" -f -n -t test-plan.jmx -l run.jtl -Jduration="$DURATION" >/dev/null 2>&1)
sleep "$SETTLE"
B_TO=$(iso)
ID_B=$(build_profile "load-${RATE}-b" "$B_FROM" "$B_TO" | tail -1)
[[ -n "$ID_B" ]] || { echo "Профиль B не сохранился" >&2; exit 1; }
echo "Профиль B: id=$ID_B"

echo "== Сравнение A↔B"
"$JAVA" -jar "$PROFILER" compare --first "$ID_A" --second "$ID_B" \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD" \
  | tee "$OUTDIR/compare.txt"

echo "== Готово: $OUTDIR/compare.txt"

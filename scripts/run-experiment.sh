#!/usr/bin/env bash
# Эксперимент целиком: прогон A (генератор), построение профиля, генерация
# плана B, прогон B и ручного плана C в JMeter, сравнение с эталоном.
# Результаты — в results/. Параметры можно переопределить:
#   DURATION=60 RATE=12 SEED=31 scripts/run-experiment.sh
set -euo pipefail

cd "$(dirname "$0")/.."

DURATION="${DURATION:-300}"
RATE="${RATE:-12}"
SEED="${SEED:-31}"
SETTLE=30
DB_URL="${DB_URL:-jdbc:postgresql://localhost:15432/profile}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# системный JAVA_HOME бывает JDK 11: выбираем первый кандидат с версией 21+
find_java() {
  local candidate
  for candidate in "${JAVA_HOME:-}" "$HOME/.jdks/temurin-21.0.12.1"; do
    if [[ -n $candidate && -x $candidate/bin/java ]] \
        && "$candidate/bin/java" -version 2>&1 | grep -q 'version "2[1-9]'; then
      echo "$candidate"
      return
    fi
  done
  echo "Нужен JDK 21: задайте JAVA_HOME или положите его в ~/.jdks" >&2
  exit 1
}
export JAVA_HOME=$(find_java)
JAVA="$JAVA_HOME/bin/java"

if [[ -x tools/apache-jmeter-5.6.3/bin/jmeter.sh ]]; then
  JMETER=$(pwd)/tools/apache-jmeter-5.6.3/bin/jmeter.sh
elif [[ -n "${JMETER_HOME:-}" && -x "$JMETER_HOME/bin/jmeter.sh" ]]; then
  JMETER="$JMETER_HOME/bin/jmeter.sh"
elif [[ -x /d/Jmeter/apache-jmeter-5.6.2/bin/jmeter.sh ]]; then
  JMETER=/d/Jmeter/apache-jmeter-5.6.2/bin/jmeter.sh
else
  echo "JMeter не найден: положите 5.6.x в tools/ или задайте JMETER_HOME" >&2
  exit 1
fi

PROFILER=profiler/target/profiler.jar
GENERATOR=demo-stand/traffic-generator/target/traffic-generator.jar
if [[ ! -f $PROFILER || ! -f $GENERATOR ]]; then
  echo "Сначала сборка: ./mvnw verify" >&2
  exit 1
fi

iso() { date -u +%Y-%m-%dT%H:%M:%SZ; }

build_profile() { # $1 имя, $2 from, $3 to
  local out
  out=$("$JAVA" -jar "$PROFILER" build-profile \
    --from "$2" --to "$3" --name "$1" \
    --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD")
  echo "$out"
  # вывод команды в консольной кодировке; ищем только ASCII-часть id=N
  echo "$out" | grep -ao 'id=[0-9]*' | head -1 | cut -d= -f2
}

mkdir -p results
docker compose up --build --detach --wait
trap 'docker compose down' EXIT

echo "== Прогон A: генератор $RATE сессий/мин, $DURATION с, зерно $SEED"
A_FROM=$(iso)
"$JAVA" -jar "$GENERATOR" --sessions-per-minute "$RATE" --duration-seconds "$DURATION" --seed "$SEED"
sleep "$SETTLE"
A_TO=$(iso)
ID_A=$(build_profile run-a "$A_FROM" "$A_TO" | tail -1)
echo "Профиль A: id=$ID_A"

echo "== План B из профиля A"
"$JAVA" -jar "$PROFILER" generate --profile-id "$ID_A" \
  --host localhost --port 18080 --out results/plan-b \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD"

echo "== Прогон B: сгенерированный план"
docker compose restart gateway catalog-service order-service >/dev/null
docker compose up --detach --wait >/dev/null
B_FROM=$(iso)
(cd results/plan-b && "$JMETER" -f -n -t test-plan.jmx -l run.jtl -Jduration="$DURATION" >/dev/null)
sleep "$SETTLE"
B_TO=$(iso)
ID_B=$(build_profile run-b "$B_FROM" "$B_TO" | tail -1)
echo "Профиль B: id=$ID_B"

echo "== Прогон C: ручной план"
docker compose restart gateway catalog-service order-service >/dev/null
docker compose up --detach --wait >/dev/null
mkdir -p results/plan-c
cp docs/experiment/plan-c/* results/plan-c/
C_FROM=$(iso)
(cd results/plan-c && "$JMETER" -f -n -t plan-c.jmx -l run.jtl -Jduration="$DURATION" >/dev/null)
sleep "$SETTLE"
C_TO=$(iso)
ID_C=$(build_profile run-c "$C_FROM" "$C_TO" | tail -1)
echo "Профиль C: id=$ID_C"

echo "== Эталон и сравнения"
REF_OUT=$("$JAVA" -jar "$PROFILER" load-profile --file docs/experiment/reference-profile.json \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD")
ID_REF=$(echo "$REF_OUT" | grep -ao 'id=[0-9]*' | head -1 | cut -d= -f2)

"$JAVA" -jar "$PROFILER" compare --first "$ID_REF" --second "$ID_B" \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD" | tee results/compare-b.txt
"$JAVA" -jar "$PROFILER" compare --first "$ID_REF" --second "$ID_C" \
  --db-url "$DB_URL" --db-user "$DB_USER" --db-password "$DB_PASSWORD" | tee results/compare-c.txt

echo "== Готово: results/compare-b.txt, results/compare-c.txt"

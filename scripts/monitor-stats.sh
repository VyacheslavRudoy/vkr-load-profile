#!/usr/bin/env bash
# Пишет docker stats в CSV каждые N секунд. Запускать в фоне:
#   scripts/monitor-stats.sh results/metrics.csv 5 &
# Остановить: kill %1
set -euo pipefail

OUTPUT=$1
INTERVAL=${2:-5}

echo "timestamp,name,cpu,mem_usage,mem_limit,net_io" > "$OUTPUT"
while true; do
    ts=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.NetIO}}" 2>/dev/null \
        | sed "s/^/$ts,/" >> "$OUTPUT"
    sleep "$INTERVAL"
done

#!/usr/bin/env bash
# Запускает все тесты плана последовательно. Каждый тест ждёт завершения
# предыдущего, стенд перезапускается между ними.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "===== ТЕСТ 1b: повторность seed 83, 12 spm"
SEED=83 RATE=12 DURATION=300 scripts/run-experiment.sh \
  > results/repetition-seed83.log 2>&1
echo "exit=$?" > results/repetition-seed83-exit.txt

echo "===== ТЕСТ 2a: нагрузка 24 spm"
RATE=24 DURATION=300 scripts/run-load-test.sh \
  > results/load-24spm/test.log 2>&1 || true

echo "===== ТЕСТ 2b: нагрузка 48 spm"
RATE=48 DURATION=300 scripts/run-load-test.sh \
  > results/load-48spm/test.log 2>&1 || true

echo "===== ТЕСТ 2c: нагрузка 96 spm"
RATE=96 DURATION=300 scripts/run-load-test.sh \
  > results/load-96spm/test.log 2>&1 || true

echo "===== ТЕСТ 2d: нагрузка 192 spm"
RATE=192 DURATION=300 scripts/run-load-test.sh \
  > results/load-192spm/test.log 2>&1 || true

echo "===== ВСЕ ТЕСТЫ ЗАВЕРШЕНЫ"

#!/usr/bin/env python3
"""Разбирает .jtl JMeter: перцентили времени ответа, ошибки, темп по операциям.

    python scripts/analyze-jtl.py results/load-24spm/plan-b/run.jtl
"""
import csv
import sys
from collections import defaultdict


def percentile(values, p):
    values = sorted(values)
    idx = int(len(values) * p / 100)
    return values[min(idx, len(values) - 1)]


def main(path):
    rows = list(csv.DictReader(open(path, encoding="utf-8", errors="replace")))
    # отсекаем родительские строки TransactionController (URL = null)
    http = [r for r in rows if r.get("URL") and r["URL"] != "null"]

    by_label = defaultdict(list)
    errors = 0
    for r in http:
        by_label[r["label"]].append(int(r["elapsed"]))
        if r["success"] == "false":
            errors += 1

    print(f"файл: {path}")
    print(f"HTTP-семплов: {len(http)}, ошибок: {errors} ({errors * 100 / max(len(http), 1):.1f}%)")
    all_elapsed = [int(r["elapsed"]) for r in http]
    if all_elapsed:
        print(f"общее время ответа (мс): P50={percentile(all_elapsed, 50)}"
              f" P90={percentile(all_elapsed, 90)} P99={percentile(all_elapsed, 99)}")
    print()
    print(f"{'операция':<45} {'n':>5} {'P50':>6} {'P90':>6} {'P99':>6}")
    print("-" * 72)
    for label in sorted(by_label):
        vals = by_label[label]
        print(f"{label:<45} {len(vals):>5} {percentile(vals, 50):>6}"
              f" {percentile(vals, 90):>6} {percentile(vals, 99):>6}")


if __name__ == "__main__":
    main(sys.argv[1])

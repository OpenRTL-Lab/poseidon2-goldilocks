<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/performance.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# v1.1.0 Performance Test Methods and Results

[简体中文](performance.md)

This document defines isolated-task latency, steady-state task throughput, and useful-operation utilization for the Goldilocks Poseidon2 accelerator, and presents ideal-interface testing, correctness checks, and measured results: the typical 16-context configuration has an isolated-task latency of 8,562 clk, a mean steady-state completion interval of 536 clk/task, and 88.0597% utilization of the shared modular multiplier.

## Test configuration and measurement conditions

The typical hardware configuration is Goldilocks Poseidon2 with a 12-field-element state (t12) and 16 context slots that store in-flight task states while sharing arithmetic. The test workload contains 256 tasks, not 256 hardware contexts. A task is a complete 12-element permutation; its tag associates request acceptance with completion. Each data-plane transfer carries the complete state. Input data is immediately available, outputs are always accepted, and compute enable remains asserted. Ideal source and sink timing directly measures accelerator computation; protocol backpressure and reset are checked separately in functional modes.

See [Reproduction guide](reproduction.en.md) for the test entry point. The verification script runs isolated and saturated modes separately and records cycle statistics and measurement logs.

## Latency and throughput are measured separately

Latency is the completion-handshake cycle minus the request-acceptance-handshake cycle for the same task, with no extra 1 added. The isolated test keeps only one task in flight: all 256 tasks have a latency of 8,562 clk. Saturated testing allows queueing and context interleaving, giving task latencies of 8,562–17,122 clk including internal waiting.

Throughput is not inferred from isolated latency. Excluding warm-up and the final batch, the measurement uses completion timestamps for tags 16 and 240. Both have the same round-robin phase; the 224 intervening task intervals span 120,064 clk.

| Metric | Result |
| --- | --- |
| Isolated-task latency | 8,562 clk |
| Saturated task-latency range | 8,562–17,122 clk |
| Mean steady-state completion interval | 536 clk/task |
| Steady-state throughput | 1/536 ≈ 0.00186567 tasks/clk |
| Isolated-task latency converted at 500 MHz | 17.124 µs |
| Task throughput converted at 500 MHz | Approximately 932,836 tasks/s |

Microseconds and tasks/s are conversions from cycle counts at 500 MHz; simulation directly measures clk. See [Synthesis results](synthesis-summary.en.md) for frequency constraints and setup data. Multiple in-flight tasks share the arithmetic pipeline, so throughput is not the reciprocal of isolated-task latency.

## Useful-operation utilization

Within the same 120,064 clk window, divide useful-operation issue counts recorded in the verification environment's `WORK` log (not a product output port) by the window length. For the shared multiplier, which issues at most once per cycle, utilization measures useful issues rather than invalid-input toggles or empty pipeline cycles. The linear networks are parallel: their figures count issue rates of logical-operation categories, not individual gate or register switching rates or power.

| Operation category | Useful operations/task | Useful issue clk in window | Idle clk in window | Utilization |
| --- | --- | --- | --- | --- |
| Shared general field modular multiplier | 472 | 105,728 | 14,336 | 88.0597% |
| External 4×4 block transformation | 9 | 2,016 | 118,048 | 1.6791% |
| External same-column cross-block combination | 9 | 2,016 | 118,048 | 1.6791% |
| Internal whole-state sum | 22 | 4,928 | 115,136 | 4.1045% |
| Internal diagonal transformation | 22 | 4,928 | 115,136 | 4.1045% |

Each seventh power requires 4 modular multiplications. The 8 full rounds process 8×12 elements and the partial rounds process 22 elements, totaling 118 seventh powers and 472 modular multiplications per task. These counts are verified per task; utilization is not approximated using total simulation duration.

This version interleaves contexts around a shared modular-multiplication pipeline with 88.0597% useful-operation utilization. Dedicated parallel linear networks account for approximately 43.72% of area; their operation issue rates are listed above. Area share and useful issue rate are measured separately.

## Correctness and total test cycles

Regenerated and bundled SV pass all documented modes with matching results. Each positive mode checks 256 tasks × 12 elements = 3072 elements against the C reference, element by element; the independent full-matrix Python model also checks every C vector.

| Mode | Total test clk | Mismatched elements | Outcome |
| --- | --- | --- | --- |
| ideal | 137,218 | 0 | Pass |
| protocol | 137,264 | 0 | Pass |
| reset | 138,219 | 0 | Pass |
| negative | 137,218 | 1 | Deliberate error detected as expected |
| isolated | 2,192,128 | 0 | Pass |
| disabled-buffer | 137,281 | 0 | Pass |

Total test cycles record the complete run, including warm-up, task admission, and the final batch. Isolated latency and steady-state throughput use the separate measurement windows defined above. The scripts generate the measurement logs locally; see [Reproduction guide](reproduction.en.md).

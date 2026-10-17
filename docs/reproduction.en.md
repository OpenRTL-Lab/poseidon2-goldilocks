<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/reproduction.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Generation and Simulation Reproduction

[简体中文](reproduction.md)

This guide gives complete steps for preparing tools, regenerating the circuit, checking reference models, verifying product SV, and reproducing cycle-performance measurements, and describes outputs, logs, configuration options, and common issues so readers can reproduce functionality and performance from source.

## Obtain the release snapshot

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-goldilocks.git
cd poseidon2-goldilocks
```

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.

## Environment preparation and command conventions

Prepare tools following [Toolchain](toolchain.en.md). Installation is a prerequisite for the scripts below, not an operation performed by the verification command:

- Python runs the scripts and independent arbitrary-precision reference model.
- A C compiler builds the reference model and must support unsigned 128-bit integers.
- JDK and Mill compile Chisel and invoke generation; the initial build resolves public Scala/Chisel dependencies.
- Verilator performs product lint and simulation; a C++ compiler and build tools compile the simulation program.
- Chisel invokes firtool, using the version specified in the toolchain document.

Run all commands at the repository root. Use new relative output directories under `build/`. Scripts do not delete or overwrite existing results; select a different directory for another run. For example, use `build/verify` first and `build/verify-second` next, retaining previous evidence.

After preparing tools, inspect the verification script's options with the following command. This only checks that the Python entry point starts and displays arguments; it does not generate a circuit, compile simulation, or verify functionality:

```bash
python3 tools/verify.py --help
```

In the commands below, `--contexts` is the number of in-flight permutation-state slots sharing the hardware arithmetic; `--tasks` is the number of test requests, not a hardware replication factor. One task permutes all 12 field elements.

## Complete end-to-end verification

The following command verifies the 16-context product: it generates reference inputs and outputs for 256 tasks, regenerates SV, and tests both regenerated and bundled SV, while measuring isolated-task latency, saturated throughput, and useful-operation utilization. `--jobs 8` only selects simulation-build parallelism; it does not change hardware configuration, data-supply conditions, or performance counts:

```bash
python3 tools/verify.py --out build/verify --contexts 16 --tasks 256 --jobs 8
```

Task count must be a power of two, at least four times the context count, and at most 4096; the default is 256. The script performs these stages:

1. Compile the independent C reference and generate 256 tasks of boundary and deterministic-random inputs and answers, with 12 elements per task.
2. Check the C model, all vectors, and known answers using the independent Python arbitrary-precision full-matrix reference.
3. Generate the product and verification environment with Chisel, extract the standalone product dependency closure, and lint it.
4. Build the regenerated-SV Verilator simulation and run six modes.
5. Compare regenerated and bundled module sets and circuit contents after removing comments; substitute bundled product SV into the same verification environment, build separately, and run all six modes.
6. Compare per-mode results from both SV sets, calculate latency, throughput, and useful-operation utilization, and write `build/verify/results.json`.

An overall exit code of 0 means every gate passed. Assertions, unexpected exit codes, reference mismatches, missing modules, or regeneration differences fail the command. The deliberate-error negative control must detect its injected error; the script treats that subprocess's exit code 1 as expected rather than failing the complete command. Consult the logs in the corresponding output directory for diagnostics. Logs and build caches are local outputs, not part of the public package. Temporary C++ build directories are cleaned up on successful or exceptional exit; reference vectors and logs remain in the selected output directory.

### Output files and interpretation

| Relative output location | Contents and use |
| --- | --- |
| `build/verify/results.json` | Generated after complete success; task and context counts, per-mode results from both SV sets, and cycle performance |
| `build/verify/configuration.json` | Generation configuration |
| `build/verify/vectors/input.hex` | Reference inputs, 12 elements per task |
| `build/verify/vectors/expected.hex` | C reference outputs, independently checked by Python |
| `build/verify/rtl/product.f` | Regenerated standalone product SV list |
| `build/verify/rtl/simulation.f` | Simulation file list including the verification environment |
| `build/verify/generated-*.log` | Lint, build, and per-mode logs for regenerated SV |
| `build/verify/bundled-*.log` | Lint, build, and per-mode logs for bundled SV, generated only for 16 contexts |
| `build/verify/c-compile.log`, `c-reference.log`, `python-reference.log`, `emit.log` | Prerequisite-stage logs in the same output directory |

Check that the complete command exits 0, `results.json` contains `status=0`, and the 16-context configuration has `bundled_matches_generated=true`. Each SV set's five positive modes must have `pass=1` and `mismatches=0`; the negative control must have `pass=0` and `mismatches=1`. The number of elements checked in each mode must equal task count multiplied by 12.

The performance fields `isolated_latency_min_cycles` / `isolated_latency_max_cycles` describe isolated latency; `cycles_per_task` and `tasks_per_cycle` express steady-state throughput; `mul_busy_fraction` and related fields count useful operations, not merely data present in a pipeline. See [Performance testing](performance.en.md) for windows, calculations, and measured values.

## Six test modes

The counts below correspond to the 256-task command above; changing task count changes the element count to tasks multiplied by 12.

| Mode | Checks | Expected outcome |
| --- | --- | --- |
| ideal | Continuous source/sink availability with starts enabled | 3072 elements, 0 errors |
| protocol | Request gaps, response backpressure, enable changes, and stable data during stalled handshakes | 3072 elements, 0 errors |
| reset | Mid-run reset, invalidation of in-flight transactions, and resubmission of all tasks | 3072 elements, 0 errors |
| negative | Deliberate one-bit flip in one reference-output element | 1 error detected; subprocess exits 1 |
| isolated | Only one task in flight | 3072 elements, 0 errors; isolated latency measurement |
| disabled-buffer | Starts disabled for the first 64 cycles; only 2 tasks can buffer and none complete during that period | 3072 elements, 0 errors |

Protocol mode is a functional gate, not an external bandwidth, latency, or backpressure sensitivity scan. Performance uses only ideal and isolated modes. The environment ROM models ideal test data supply; it is not an implementation of the product's external memory.

## Generate SV only

Use the following command when only circuit generation and product file lists are required. It generates reference vectors, checks them using Python, then invokes Chisel for the product and verification environment; it does not run the complete Verilator regression or produce complete performance results:

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --tasks 256
```

`build/emit/rtl/product.f` lists standalone product SV using relative paths. `build/emit/rtl/simulation.f` additionally includes the verification environment and wrapper. Use `product.f` for integration; do not synthesize the verification ROM and clock driver with the product. `--contexts` changes the hardware context count; `--tasks` controls verification tasks and vectors, not the mathematical permutation definition.

To check only the bundled product SV's syntax and structure without regeneration or input/output comparison, run product lint below. It is not a complete functional or performance test:

```bash
verilator --lint-only --assert --top-module GoldilocksPoseidon2 -f rtl/v1.1.0/t12-ctx16/files.f
```

## 32-context configuration

The command below generates and verifies 32 contexts, including reference checks, product lint, all six functional modes, and ideal-interface cycle measurements. Since bundled SV uses 16 contexts, it does not compare different configurations against each other:

```bash
python3 tools/verify.py --out build/verify-ctx32 --contexts 32 --tasks 256 --jobs 8
```

The 32-context configuration passes functional verification with 256 tasks. The 500 MHz synthesis summary applies to 16 contexts, not to the 32-context option.

## Troubleshooting

- **Missing or incompatible tools**: check commands and dependencies against the toolchain document. Use its `MILL` environment-variable instructions if the launcher has another name.
- **Initial dependency download failure**: check network access to public dependency sources and read/write permissions for user-selected caches. The repository binds neither proxies nor cache directories.
- **Existing output directory**: choose a fresh relative `build/` directory. Do not treat results in an old directory as evidence for a new run.
- **Invalid options**: context count is 16 or 32; task count is a power of two, at least four times context count, and at most 4096; `--jobs` is between 1 and 64.
- **Reference, lint, or simulation failure**: consult the appropriate stage log. Partial outputs after command failure do not establish complete verification success.
- **Generation alone succeeds**: emit exit code 0 establishes successful generation and prerequisite reference checks. Run complete verification to establish functionality and cycle performance.

Latency, throughput, and utilization are measured in RTL simulation cycles; time and tasks/s are converted at the specified frequency. See [Synthesis results](synthesis-summary.en.md) for synthesis conditions and measurements of the bundled 16-context configuration.

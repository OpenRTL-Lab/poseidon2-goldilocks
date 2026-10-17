<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: README.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Goldilocks Poseidon2 Permutation Hardware Accelerator

[简体中文](README.zh-CN.md)

This repository provides a standalone Goldilocks Poseidon2 hardware accelerator developed in **Chisel**. Through its native interface, it can be integrated into ASIC or FPGA designs to accelerate the underlying permutation operations used in Merkle commitments, authentication-path verification, Fiat–Shamir challenge generation, and other computations in zero-knowledge systems with the same permutation parameters.

Licensed under the [MIT License](LICENSE), permitting free commercial use, modification, and redistribution, provided the copyright and license notices are retained; see [Third-party notices](THIRD_PARTY_NOTICES.en.md) for the third-party parameters.

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0) · [Native interface](docs/interface.en.md) · [Reproduction guide](docs/reproduction.en.md) · [Performance testing](docs/performance.en.md) · [Synthesis results](docs/synthesis-summary.en.md)

## Computation and task units

One request is one complete permutation of a 12-field-element state, and one response returns all 12 permuted elements atomically with the caller's 32-bit tag. The configuration name t12 denotes that state width. Each element is a canonical unsigned 64-bit Goldilocks value, 0 <= x < p with p = 2^64 - 2^32 + 1; this is a permutation interface, not a padded message-hash interface.

A context is a stored in-flight permutation state. The 16 contexts interleave work through shared arithmetic pipelines; they are not 16 independent permutation cores. This overlap makes a steady completion interval much shorter than the latency of an isolated request. The enable signal controls starting queued requests, not the progress of already active contexts.

## At a glance

| Integration question | Released hardware |
| --- | --- |
| Computation | Complete 12-element Poseidon2 permutation; seventh-power S-box, 8 full and 22 partial rounds |
| Field and encoding | Goldilocks p = 2^64 - 2^32 + 1; 12 canonical 64-bit elements per state |
| Bundled configuration | t12, 16 interleaved contexts; top `GoldilocksPoseidon2` |
| Native boundary | Atomic 12-element ready/valid request and response, with a returned 32-bit tag |
| Other documented configuration | 32 contexts are separately verified; bundled synthesis results cover 16 contexts |

## Quick start

Download the source archive from [Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0), or clone the current release snapshot:

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-goldilocks.git
cd poseidon2-goldilocks
```

Install the exact prerequisites in [Toolchain](docs/toolchain.en.md), then run from the repository root with a fresh output directory:

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --tasks 256
```

Here --contexts selects hardware state slots; --tasks selects the verification workload size, not the number of hardware cores. This emission command is not a complete RTL regression. The documented verification entry is:

```bash
python3 tools/verify.py --out build/verify --contexts 16 --tasks 256 --jobs 8
```

Follow [Reproduction guide](docs/reproduction.en.md) for expected results, negative-control interpretation and the distinction between short functional and full performance runs.

## Verification and integration boundary

The recorded flow cross-checks independent C and Python references and runs regenerated and bundled SV. Six modes distinguish ideal traffic, protocol stalls, reset recovery, an intentional one-bit reference error, isolated latency and buffering requests while enable is low, then starting them after re-enable. In positive 256-task modes every one of the 3,072 result elements is checked; the negative mode must detect its deliberate error.

This is a state-to-state permutation, not a complete sponge, byte hash, Merkle engine or transcript. Absorption, padding, domain separation and digest extraction remain external. All round constants and linear layers must match the target protocol. Ready/valid handshakes and tags, rather than a fixed response delay, define integration; reset invalidates in-flight work.

### Reading performance correctly

With ideal input/output timing and enable asserted, an isolated permutation takes 8,562 cycles; with 16 in-flight state slots in use, successive results complete every 536 cycles in the measured steady window. The difference comes from interleaving, not a 536-cycle single-request latency. Shared-multiplier useful-issue utilization is 105,728 valid issues / 120,064 steady-window cycles = 88.0597%, not a whole-chip activity measure.

These are documented RTL cycle measurements under ideal native interfaces. MHz-based time/rate conversions are not board or silicon measurements. The reported Design Compiler results apply only to the documented configuration, library corner and constraints; they do not establish place-and-route signoff or system throughput. See [Performance testing](docs/performance.en.md) and [Synthesis results](docs/synthesis-summary.en.md) for conditions.

## Repository contents

- **Chisel design**: [operators/poseidon2/src/main/scala/zk/poseidon2/](operators/poseidon2/src/main/scala/zk/poseidon2/) contains arithmetic pipelines, linear transformations, and multicontext scheduling. [Environment.scala](operators/poseidon2/src/main/scala/zk/poseidon2/Environment.scala) is a verification environment, not part of the product circuit.
- **Generated SV**: the [Product SV directory](rtl/v1.1.0/t12-ctx16/) contains a 12-field-element design with 16 interleaved contexts. The top module is [GoldilocksPoseidon2](rtl/v1.1.0/t12-ctx16/GoldilocksPoseidon2.sv); [files.f](rtl/v1.1.0/t12-ctx16/files.f) lists the product files needed for integration.
- **Reference models and parameters**: [operators/poseidon2/cmodel/](operators/poseidon2/cmodel/) provides the independent C reference model; [operators/poseidon2/reference/](operators/poseidon2/reference/) contains fixed parameters and known answers; [operators/poseidon2/tools/check_reference.py](operators/poseidon2/tools/check_reference.py) checks the reference results using an independent Python arbitrary-precision model.
- **Generation and verification tools**: [tools/emit_sv.py](tools/emit_sv.py) generates the circuit; [tools/verify.py](tools/verify.py) performs reference checks, RTL simulation, and performance measurements; [operators/poseidon2/sim/](operators/poseidon2/sim/) provides the C++ simulation clock driver.
- **Design and integration documentation**: [Circuit function and mathematical model](docs/mathematical-model.en.md), [Hardware architecture](docs/architecture.en.md), [Native interface](docs/interface.en.md), [Toolchain](docs/toolchain.en.md), and [Reproduction guide](docs/reproduction.en.md).
- **Performance and synthesis information**: [Performance testing](docs/performance.en.md) defines isolated-task latency, saturated throughput, and useful-operation utilization; [Synthesis results](docs/synthesis-summary.en.md) describes the process, constraints, area, setup timing, and logic-depth results.

## Applications

When a system uses a 12-element Poseidon2 state over the Goldilocks field for hash compression, commitments, or challenge generation, its complete permutation operations can be offloaded to this module, with matching round constants, linear layers, and round counts. The module computes a state-to-state permutation; the integrator supplies protocol operations such as message absorption and digest extraction. The following are examples, not an exhaustive list or a restriction to a particular proof framework:

- **Merkle commitments in STARK provers**: offload the permutations used by leaf hashing and parent compression when committing to execution traces, quotient polynomials, or FRI round data. Tree scheduling and storage remain external. Plonky3's [Merkle tree implementation](https://github.com/Plonky3/Plonky3/blob/main/merkle-tree/src/merkle_tree.rs) distinguishes these two computations.
- **Fiat–Shamir challenge generation in provers and verifiers**: perform duplex-sponge permutations after absorbing public inputs, commitments, and messages, or when producing new challenges. Absorption, padding, domain separation, and output order follow the target protocol. See Plonky3's [DuplexChallenger](https://github.com/Plonky3/Plonky3/blob/main/challenger/src/duplex_challenger.rs).
- **Merkle authentication-path verification**: reuse the permutation module to recompute leaf digests and successive parent compressions before comparing the result with the commitment root. Path management and root comparison remain external.

These examples illustrate some integration points. Integrators can combine absorption and output operations, padding, domain separation, and tree scheduling to use the module in other computation chains with the same permutation parameters.

## Performance metrics

Under ideal-interface simulation, the typical 16-context configuration achieves an isolated-task latency of 8,562 clk, a steady-state mean completion interval of 536 clk/task (17.124 µs and approximately 932,836 tasks/s at 500 MHz), and 88.0597% useful-operation utilization of the shared modular multiplier; the detailed document defines the metrics, test methods, and measured results. See [Performance testing](docs/performance.en.md).

## Mathematical model

For each task, the circuit performs a 12-element Poseidon2 permutation over the field with modulus $p=2^{64}-2^{32}+1$, using a seventh-power nonlinear layer, 8 full rounds, and 22 partial rounds, and returns the original 32-bit task tag; the detailed document specifies the fixed constants and every round precisely. See [Circuit function and mathematical model](docs/mathematical-model.en.md).

## Quick reproduction

The reproduction flow covers tool preparation, SV generation, reference-model comparison, functional verification, and ideal-interface cycle-performance testing. See [Reproduction guide](docs/reproduction.en.md).

## Synthesis results

The bundled configuration has no setup violations under 500 MHz and 10% setup clock uncertainty at the TSMC 28 nm HPC+ SSG corner, with approximately 0.142350 mm² mapped cell area and 99.7371% of the worst setup paths per endpoint strictly below 50 logic levels. See [Synthesis results](docs/synthesis-summary.en.md).

## Publication scope

Complete code is provided to reproduce the Chisel design, SV generation, reference models, functional verification, and ideal-interface cycle-performance tests. Performance tests introduce no external bandwidth or memory-latency limitations; users evaluate data supply and integration performance in their own systems.

We apologize that third-party licensing and engineering confidentiality restrictions prevent us from providing the process libraries, original DC Tcl/SDC, raw reports, mapped netlists, or synthesis snapshots used in our synthesis flow. For implementation reference, [Synthesis results](docs/synthesis-summary.en.md) reports the tool version, process conditions, constraints, and key results. RTL generation and simulation verification are publicly reproducible; the synthesis figures apply to the bundled 16-context configuration under the documented constraints.

This project delivers a standalone permutation module. SoCs, bus adapters, software drivers, board workflows, sponge protocols, digest extraction, Merkle-tree protocols, and complete STARK systems are part of the integrator's system design. Users can provide protocol wrappers and system integration for their needs; see the [MIT License](LICENSE).

## OpenRTL and custom integration

Browse [OpenRTL](https://github.com/OpenRTL-Lab), the [ZK accelerator catalogue](https://github.com/OpenRTL-Lab/zk-accelerators) and [OpenRTL website](https://openrtl.top/) for related modules and the cooperation/contact entry. Custom datapaths, interfaces and target-platform adaptation are optional services; MIT commercial use does not require buying a service. Do not submit confidential specifications in public issues.

## Release sources and integration

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.

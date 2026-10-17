<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/architecture.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# v1.1.0 Architecture and Precise Definition

[简体中文](architecture.md)

This document describes the fixed operations, arithmetic pipelines, linear transformations, multicontext scheduling, generation options, and product boundary of the 12-element Goldilocks Poseidon2 accelerator; interleaved contexts share the modular-multiplication pipeline to advance multiple tasks concurrently.

## Mathematical boundary

The field is `F_p`, with `p = 2^64 - 2^32 + 1`. All 12 input elements must be in `[0,p)`, and outputs use the same canonical representation. The fixed Goldilocks Poseidon2 parameters are in [parameters.json](../operators/poseidon2/reference/parameters.json); the S-box is `x^7`. See [Circuit function and mathematical model](mathematical-model.en.md) for the complete state-to-state definition.

The round order is an initial external matrix transformation, 4 full rounds, 22 partial rounds, and 4 full rounds. Each full round adds constants to all 12 elements, raises each to the seventh power, and applies the external matrix. Each partial round adds a constant and applies the seventh power only to element 0, then applies the internal matrix to the whole state.

Split the state into three consecutive four-element blocks. Each uses this 4×4 matrix:

```text
2 3 1 1
1 2 3 1
1 1 2 3
3 1 1 2
```

Transform each block first. Each output then equals its own block result plus the sum of the corresponding results from all three blocks, with all arithmetic in the field. Equivalently, the full 12×12 matrix uses twice the above matrix for same-block coefficients and once for cross-block coefficients.

The internal transformation is `y_i = sum(x_j) + diagonal_i * x_i mod p`, with diagonal coefficients fixed by the same parameter file. That file also provides the known answer for input `[0,1,...,11]`, checked by the C model, Python model, and RTL.

A task is one complete 12-element permutation. A context is the state slot that retains one such task while other tasks use the shared arithmetic; the context count is not the number of independent permutation cores.

## Hardware organization

The 16 contexts share one round-robin scheduler, one general field modular-multiplication pipeline, and dedicated linear-transformation networks. Each context stores 12 state elements, a tag, round and word indices, and nonlinear intermediate values. Each seventh power uses four useful modular multiplications to compute the square, cube, fourth power, and seventh power. The arithmetic units are internally pipelined; interleaving hides feedback waits rather than replicating an entire task engine 16 times.

The input and output queues each have depth 2 and no combinational bypass. Scheduler reads and arithmetic are separated by explicit registers, and feedback completes before the next access to the same context. The multiplier arithmetic body has latency 11 cycles and initiation interval 1 cycle. Including selection, registers, and alignment, the nonlinear context-feedback path takes 15 cycles. The linear-feedback path includes a read register and a 9-cycle linear pipeline, totaling 10 cycles. These cycle counts describe the RTL structure, not mapped combinational logic depth.

Generation supports 16 or 32 contexts. This version does not expose the field, state width, or round parameters as configuration options. Bundled SV and synthesis results cover 16 contexts. The 32-context option can be generated and has functional verification; it uses its own configuration-specific area, timing, and performance results rather than the 16-context figures.

## Product and verification environment separation

The product contains no FPGA primitives or process macros; states and queues are described in generic RTL. The name `ram_2x800` denotes queue-storage RTL, not a proprietary SRAM macro. In the reported synthesis, this configuration maps to standard cells without physical memory macros.

`Environment.scala` supplies test-ROM inputs, checks C reference answers, and emits verification logs. It is a simulation environment and is excluded from the standalone product file list. `WORK` records useful scheduler operations for simulation utilization measurements; it is not a product observation counter.

The complete script first generates the matching verification environment, then extracts the dependency closure of product modules to reproduce the bundled product SV. Only source-location comments are removed from public SV. Regenerated and bundled products are compared module by module after comment removal, and then simulated separately.

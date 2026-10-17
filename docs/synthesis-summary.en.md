<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/synthesis-summary.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# v1.1.0 Synthesis Conditions and Reported Results

[简体中文](synthesis-summary.md)

This document reports the DC logic-synthesis tool, process conditions, constraints, setup timing, area, and logic-depth results for the bundled 12-element, 16-context design: it passes 500 MHz with 10% setup clock uncertainty at the TSMC 28 nm HPC+ SSG corner, with approximately 0.142350 mm² mapped cell area and 99.7371% of the worst setup paths per endpoint strictly below 50 logic levels.

We apologize that third-party licensing and engineering confidentiality restrictions prevent publication of the original DC Tcl, SDC, process libraries, mapped netlists, timing reports, or synthesis snapshots used in the flow. This document reports the tools, process, constraints, and key results in text. Complete code is provided for RTL generation and simulation verification; see [Reproduction guide](reproduction.en.md).

## Design and conditions

The design is `GoldilocksPoseidon2`, t12, 16 contexts, corresponding to the bundled v1.1.0 product SV. The flow uses Synopsys Design Compiler NXT X-2025.06-SP3 and TSMC 28 nm HPC+ standard cells. The designated worst process corner for this run is SSG, 0.81 V, −40 °C. There are no physical SRAM macros or black boxes; queues and context states map to standard cells.

| Constraint | Reported condition |
| --- | --- |
| Operating frequency/period | 500 MHz / 2.0 ns |
| Setup clock uncertainty | 0.2 ns, or 10% of the period |
| Maximum input/output delay | 1.0 ns |
| Minimum input/output delay | 0 ns |
| Clock transition | 0.10 ns |
| Input transition | 0.125 ns |
| Maximum data transition | 0.25 ns |
| Output load | 0.02, in library-defined load units |
| Reset | Synchronous active high; reset data paths are included in setup analysis |

The setup clock-uncertainty budget is 0.2 ns, equal to 10% of the 2.0 ns period. The gates for this run are setup and combinational logic-depth distribution.

Here t12 means a state of 12 field elements; the 16 contexts are stored in-flight task states sharing arithmetic. WNS is the minimum setup slack, while TNS sums negative setup slacks. Reported cell area excludes routing and placement whitespace and is not die area.

## Setup and area

| Metric | Reported result |
| --- | --- |
| Setup WNS | +0.000125289 ns |
| Setup TNS | 0 ns |
| Violating endpoints | 0 |
| Constrained setup endpoints | 49,444 |
| Total cell area | 142,349.999 µm², approximately 0.142350 mm² |
| Combinational logic, excluding buffers/inverters | 57,619.982 µm² |
| Buffers/inverters | 3,692.444 µm² |
| Noncombinational logic | 81,037.572 µm² |
| Memory macros/black boxes | 0 |

Setup passes with WNS +0.000125289 ns (approximately +0.125 ps), TNS 0 ns, and 0 violating endpoints. Area is the synthesis-mapped cell area. Including descendants, the linear layer accounts for approximately 43.72%, the S-box layer 8.71%, and its general modular multiplier 6.27%. Parent areas include descendant areas; interpret these figures hierarchically.

## Combinational logic depth

Take the worst max path for each constrained setup endpoint: 49,444 paths in total, comprising 33,980 internal-to-internal, 802 internal-to-IO, and 14,662 IO-to-internal paths. Count one logic level only for data-path points whose leaf pin name is exactly `Z` or `ZN`. Report lines, other pins, net rows, clock-to-Q, and clock-tree points do not count as logic levels. The statistical population is the worst max path per constrained endpoint.

| Strict threshold | Count | Share of endpoint-worst-path population |
| --- | --- | --- |
| >30 levels | 794 | 1.6059% |
| >40 levels | 351 | 0.7099% |
| >50 levels | 115 | 0.2326% |
| >60 levels | 33 | 0.0667% |

The maximum is 67 levels, and 15 paths have exactly 50 levels. There are 49,314 paths strictly below 50 levels, or 99.7371%. Threshold counts are cumulative and overlap. These data correspond to DC logic synthesis of the configuration above; the reported metrics are setup, mapped cell area, and logic depth of worst max paths per endpoint.

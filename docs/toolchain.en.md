<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/toolchain.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Toolchain Versions

[简体中文](toolchain.md)

This document lists the tool versions used for circuit generation, reference-model checks, and functional and performance reproduction, and explains installation, dependency resolution, launch commands, and environment variables for a consistent reproduction environment; the project build fixes Scala, Chisel, and compiler-plugin versions.

| Tool | Version/requirement | Purpose |
| --- | --- | --- |
| Mill | 1.1.7 | Scala/Chisel build |
| Scala | 2.13.18 | Design language |
| Chisel and compiler plugin | 7.13.0 | Circuit generation |
| CIRCT firtool | 1.149.0 | SystemVerilog lowering and splitting |
| OpenJDK | 21.0.11 | JDK used for reproduction |
| Python | 3.12 | Scripts and independent arbitrary-precision reference |
| Verilator | 5.050 | Product lint and RTL simulation |
| GCC/G++ | 16.2.0 | C reference and C++ simulation builds |

Install these open-source tools so that `java`, `mill`, `python3`, `verilator`, `cc`, and `g++` are available on PATH. Mill resolves Scala, Chisel, and the compiler plugin from public Maven dependencies; the first build needs network access or a dependency cache prepared by the user. Use the public firtool resolution and installation flow associated with Chisel; retain the documented firtool version for this reproduction flow. Machine-specific launchers, tool binaries, proxy addresses, and cache directories are not bundled.

If the Mill launcher has a different name, set the `MILL` environment variable to the launch command, for example `MILL='bash mill-launcher'`. Optionally, `CC` selects the C compiler. Users choose cache locations through their own environment; project scripts do not bind them to a machine. Verification needs a writable temporary directory; set `TMPDIR` if required by the environment. The tested combination is listed above. After changing tool versions, rerun complete verification and record the corresponding timing and performance results.

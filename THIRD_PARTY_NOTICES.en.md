<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: THIRD_PARTY_NOTICES.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Third-party Parameters and Licensing

[简体中文](THIRD_PARTY_NOTICES.md)

This document describes the public sources of the fixed Poseidon2 parameters and known answers, the selected MIT licensing option, and the copyright and license notices to retain when distributing the source or generated circuit, supporting commercial use, modification, and redistribution under the applicable licenses.

The fixed Goldilocks Poseidon2 parameters and known answers originate from the public [Plonky3 project](https://github.com/Plonky3/Plonky3). The actual values are included in this repository: no runtime access to upstream is required, and upstream changes are not adopted automatically.

The public parameter source offers MIT or Apache-2.0 licensing options. This project uses the MIT option for those parameters and retains the [Original MIT copyright and license](operators/poseidon2/reference/LICENSE-MIT). The parameter-bearing files include `Parameters.scala`, `parameters.h`, `parameters.json`, and the SV constants generated from them.

The project's own Chisel design, generated SV, reference implementations, verification code, tools, and documentation use the root [MIT License](LICENSE). Third-party parameters retain their original copyright and MIT notices; these must not be replaced with or removed in favor of the project's copyright notice. Distributions of source, generated SV, or other substantial portions must retain the applicable copyright and license notices.

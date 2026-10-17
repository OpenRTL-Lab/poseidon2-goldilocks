<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: THIRD_PARTY_NOTICES.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 第三方参数与许可

[English](THIRD_PARTY_NOTICES.en.md)

本文件说明固定 Poseidon2 参数及已知答案的公开来源、采用的MIT许可选项，以及分发源码和生成电路时应保留的版权与许可声明，便于使用者遵循许可要求进行商用、修改和再分发。

固定Goldilocks Poseidon2参数及已知答案来自公开的 [Plonky3 项目](https://github.com/Plonky3/Plonky3)。
本仓库保留了使用的实际数值，不要求运行时访问上游，也不跟随上游自动更新。
公开参数来源提供MIT或Apache-2.0许可选择；本项目对应参数采用MIT选项，并保留 [原MIT版权及许可正文](operators/poseidon2/reference/LICENSE-MIT)。
涉及参数的文件包括 `Parameters.scala`、`parameters.h`、`parameters.json`，以及由这些参数生成的SV常量。

本项目自研Chisel、生成的SV、参考实现、验证代码、工具脚本和文档采用根目录的 [MIT 许可证](LICENSE)。
第三方参数仍保留其原有版权及MIT声明，不能用本项目版权声明替换或删除。
分发源码、生成的SV或其他实质性部分时，应一并保留适用的版权和许可声明。

<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/toolchain.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 工具链版本

[English](toolchain.en.md)

本文件列出已用于电路生成、参考模型核对和功能/性能复测的工具版本，说明安装、依赖解析、启动命令和环境变量，帮助读者建立一致的复现环境；Scala/Chisel及插件版本由项目构建文件固定。

| 工具 | 版本/要求 | 用途 |
| --- | --- | --- |
| Mill | 1.1.7 | Scala/Chisel构建 |
| Scala | 2.13.18 | 设计语言 |
| Chisel与编译插件 | 7.13.0 | 电路生成 |
| CIRCT firtool | 1.149.0 | SystemVerilog降级与拆分 |
| OpenJDK | 21.0.11 | 验证使用的 JDK |
| Python | 3.12 | 脚本、独立大整数参考 |
| Verilator | 5.050 | 产品lint和RTL仿真 |
| GCC/G++ | 16.2.0 | C参考与C++仿真构建 |

先安装上述开源工具，使 `java`、`mill`、`python3`、`verilator`、`cc`、`g++` 可从 PATH 调用。
Scala/Chisel及插件由 Mill 通过公开 Maven 依赖解析；首次使用需要正常网络或用户自己准备的依赖缓存。
firtool可以使用 Chisel 对应的公开解析安装流程；不要把不同版本的 firtool 强制替换到本版本复现流程中。
不随包提供机器专用启动器、工具二进制、代理地址或缓存目录。

若 Mill 不是名为 `mill` 的可执行启动器，可通过环境变量 `MILL` 指定启动命令；例如 `MILL='bash mill-launcher'`。
可选 `CC` 指定C编译器。缓存位置由用户环境选择，不在项目脚本内绑定任何机器。
验证中间文件需要可写的临时目录；环境限制时可由用户设置 `TMPDIR`。
已验证的工具组合如上表；工具版本变更后，重新运行完整验证并记录对应的时序与性能结果。

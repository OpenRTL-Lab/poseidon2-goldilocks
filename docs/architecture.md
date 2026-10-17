<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/architecture.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# v1.1.0 架构与精确定义

[English](architecture.en.md)

本文件介绍12元素 Goldilocks Poseidon2 加速器的固定运算、算术流水线、线性变换和多上下文调度，以及生成配置和产品边界；通过上下文交织复用模乘流水线，实现多个任务的并发推进。

## 数学边界

域为 `F_p`，`p = 2^64 - 2^32 + 1`。输入12个元素均须处于 `[0,p)`，输出同样规范化。
采用 [parameters.json](../operators/poseidon2/reference/parameters.json) 固定的 Goldilocks Poseidon2 参数，S-box 为 `x^7`；状态到状态的完整定义见 [电路功能与数学模型](mathematical-model.md)。

轮顺序为：一次初始外部矩阵变换；4个完整轮；22个部分轮；4个完整轮。
完整轮对12个元素分别加轮常量、做七次幂，再做外部矩阵变换。
部分轮只对元素0加轮常量并做七次幂，再对整个状态做内部矩阵变换。

把状态分为三个连续的4元素块。每块使用4×4矩阵：

```text
2 3 1 1
1 2 3 1
1 1 2 3
3 1 1 2
```

对每个块先得到变换结果；随后每个输出等于本块结果加三个块同位置结果之和，全部在域内运算。
等价完整12×12矩阵的同块系数为上述矩阵的两倍、异块系数为一倍。
内部变换为 `y_i = sum(x_j) + diagonal_i * x_i mod p`，对角系数由同一参数文件固定。
该参数文件同时提供输入 `[0,1,...,11]` 的已知答案；C/Python/RTL全部核对。

一个任务是一遍完整的12元素置换。上下文是保存该任务的状态槽，让其他任务能在其等待反馈时使用共享算术；上下文数不是独立置换核的数量。

## 硬件组织

16个上下文共享一个轮转调度器、一个通用域模乘流水线及专用线性变换网络。
每个上下文保存12个状态元素、标签、轮次、字索引及非线性计算中间值。
每次七次幂通过平方、立方、四次方和七次方四次有效模乘完成。
算术单元有内部流水线，上下文交织隐藏反馈等待；不是把整个任务复制16份。

输入与输出各有深度2的队列，无组合穿透。
调度读取与算术之间有明确寄存边界，反馈在下一次访问同一上下文前完成。
模乘算术体延迟11拍、发射间隔1拍；上下文非线性反馈路径包含选择、寄存与对齐，共15拍。
线性反馈路径含读取寄存与9拍线性流水线，共10拍。
这些拍数是RTL结构定义，不是映射后的组合逻辑级数。

生成参数仅支持上下文数16或32，域、状态宽度和轮参数不在本版本中开放配置。
随包SV和综合摘要仅覆盖16上下文；32上下文可生成并通过功能验证，不套用16上下文的面积、时序或性能数据。

## 产品与验证环境分离

产品模块不含 FPGA 原语或工艺宏；状态和队列由通用RTL描述。名称中的 `ram_2x800` 是队列存储RTL，不是专有SRAM宏。
综合转述中该配置实际映射为标准单元，没有物理存储宏。
`Environment.scala` 用测试ROM喂数、比较C参考答案并输出验证日志，属于仿真环境，不纳入独立产品文件列表。
`WORK` 记录调度器有效操作，供仿真计算忙率；不是产品观测计数器。
完整脚本先生成配套验证环境，再提取产品模块依赖闭包，以便复现本版本既有产品SV。
公开SV仅去掉源码定位注释；重新生成的产品与随包产品逐模块比较去注释的电路内容，随后分别仿真。

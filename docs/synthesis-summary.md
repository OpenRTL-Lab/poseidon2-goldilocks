<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/synthesis-summary.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# v1.1.0 综合条件与结果转述

[English](synthesis-summary.en.md)

本文件汇总随包12元素状态、16上下文设计的DC逻辑综合工具、工艺条件、约束及setup、面积和逻辑级数结果：TSMC 28 nm HPC+ SSG工艺角下，500 MHz和10% setup时钟不确定度约束通过，映射cell面积约0.142350 mm²，99.7371%的端点最差setup路径严格小于50级逻辑。

很抱歉，出于第三方授权和工程资料保密方面的限制，我们无法公开已有综合流程使用的原始DC Tcl、SDC、工艺库、映射网表、时序报告或综合快照。这里提供工具、工艺、约束及关键结果的文字转述；RTL生成与仿真验证提供完整代码，复现入口见 [复现指南](reproduction.md)。

## 设计及条件

设计为 `GoldilocksPoseidon2`，t12、16上下文，对应随包v1.1.0产品SV。
使用 Synopsys Design Compiler NXT X-2025.06-SP3，TSMC 28 nm HPC+标准单元库。
该配置的最差工艺角为 SSG、0.81 V、−40 °C。
无物理SRAM宏或Black Box，队列和上下文状态映射为标准单元。

| 约束项目 | 转述条件 |
| --- | --- |
| 工作频率/周期 | 500 MHz / 2.0 ns |
| Setup时钟不确定度 | 0.2 ns，即周期的10% |
| 输入/输出最大延迟 | 1.0 ns |
| 输入/输出最小延迟 | 0 ns |
| 时钟转换时间 | 0.10 ns |
| 输入转换时间 | 0.125 ns |
| 最大数据转换时间 | 0.25 ns |
| 输出负载 | 0.02，按库定义的负载单位 |
| 复位 | 同步高有效，不把复位数据路径排除在setup外 |

Setup时钟不确定度预算为0.2 ns，等于2.0 ns周期的10%。
验收指标为setup和组合逻辑级数分布。

这里 t12 表示12个域元素的状态；16个上下文是共享算术的在途任务状态槽。WNS 为最差 setup 裕量，TNS 为负 setup 裕量之和。所报 cell 面积不含布线和布局留白，不是芯片 die 面积。

## Setup与面积

| 指标 | 转述结果 |
| --- | --- |
| Setup WNS | +0.000125289 ns |
| Setup TNS | 0 ns |
| 违例端点 | 0 |
| 已约束setup端点 | 49,444 |
| 总cell面积 | 142,349.999 µm²，约0.142350 mm² |
| 组合逻辑，不含Buf/Inv | 57,619.982 µm² |
| Buf/Inv | 3,692.444 µm² |
| 非组合逻辑 | 81,037.572 µm² |
| 存储宏/Black Box | 0 |

Setup通过，WNS为+0.000125289 ns（约+0.125 ps），TNS为0 ns，违例端点为0。
面积统计口径为综合映射cell面积。
含子层次面积中，线性层约占43.72%，S-box层约8.71%，其中通用模乘约6.27%；父层次面积包含子层次面积，按包含关系解读。

## 组合逻辑级数

每个已约束setup端点取其最差的一条max路径，共49,444条：
内部→内部33,980条、内部→IO 802条、IO→内部14,662条。
仅数据路径中叶引脚名严格为 `Z` 或 `ZN` 的点计一级；不按报告行数、其他引脚、net行、clock-to-Q或时钟树计级。
统计集合为每个已约束端点的最差max路径。

| 严格阈值 | 数量 | 占端点最差路径集合 |
| --- | --- | --- |
| >30级 | 794 | 1.6059% |
| >40级 | 351 | 0.7099% |
| >50级 | 115 | 0.2326% |
| >60级 | 33 | 0.0667% |

最大67级，恰好50级15条；严格小于50级为49,314条，占99.7371%。
阈值计数为累计分布，各行存在包含关系。
该配置数据集对应上述配置的DC逻辑综合；统计项目为setup、映射cell面积与端点最差max路径的逻辑级数。

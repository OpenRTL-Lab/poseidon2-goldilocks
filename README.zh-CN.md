<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: README.zh-CN.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# poseidon2-goldilocks 硬件加速器

[English](README.md)

本仓库提供使用 **Chisel** 开发的 Goldilocks Poseidon2 独立硬件加速器。它可通过原生接口集成到 ASIC 或 FPGA，加速采用相同置换参数的零知识证明系统中 Merkle 承诺、认证路径核验、Fiat–Shamir 挑战生成等计算所需的底层置换运算。

采用 [MIT 许可证](LICENSE)，允许免费商用、修改和再分发，须保留版权及许可声明；第三方参数声明见 [第三方声明](THIRD_PARTY_NOTICES.md)。

[发布 v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0) · [原生接口](docs/interface.md) · [复现指南](docs/reproduction.md) · [性能测试](docs/performance.md) · [综合结果](docs/synthesis-summary.md)

## 计算与任务单位

一次请求是一遍完整的12域元素状态置换，一次响应原子返回全部12个置换后元素和调用方原32位标签；t12表示状态宽度为12。每个元素是无符号64位Goldilocks规范值，即p = 2^64 - 2^32 + 1下的0 ≤ x < p；这是置换接口，不是带填充规则的消息哈希接口。

上下文是保存一项在途置换状态的槽位。16个上下文交错使用共享算术流水线，不是16套独立置换核；交织使稳态完成间隔远短于孤立请求延迟。enable只控制队列内请求能否启动，不控制已经运行的上下文进度。

## 配置速览

| 集成问题 | 已发布硬件 |
| --- | --- |
| 计算 | 完整12元素Poseidon2置换；七次幂S-box、8个完整轮和22个部分轮 |
| 数域与编码 | Goldilocks p = 2^64 - 2^32 + 1；每状态12个规范64位元素 |
| 随包配置 | t12、16个交织上下文；顶层`GoldilocksPoseidon2` |
| 原生边界 | 原子12元素ready/valid请求与响应，原样返回32位标签 |
| 其他已记录配置 | 32上下文另有验证；随包综合结果覆盖16上下文 |

## 快速开始

从 [发布 v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0) 下载源码包，或克隆当前发布快照：

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-goldilocks.git
cd poseidon2-goldilocks
```

按 [工具链说明](docs/toolchain.md) 安装精确版本的前置工具，然后在仓库根目录使用全新输出目录运行：

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --tasks 256
```

这里--contexts选择硬件状态槽数，--tasks选择验证工作负载大小，不是硬件核数量。该生成命令不是完整RTL回归；文档规定的验证入口为：

```bash
python3 tools/verify.py --out build/verify --contexts 16 --tasks 256 --jobs 8
```

预期结果、负控判读以及短功能运行与完整性能运行的区别见 [复现指南](docs/reproduction.md)。

## 验证与集成边界

已记录的流程交叉核对独立C/Python参考，并运行重新生成及随包SV。六种模式区分理想流量、协议停顿、复位恢复、故意翻转参考一位、孤立延迟，以及enable低时缓冲请求并在重新使能后启动。256任务正向模式逐一检查全部3,072个结果元素；负控必须检出故意加入的错误。

模块提供状态到状态的置换，不是完整sponge、字节哈希、Merkle引擎或transcript。吸收、填充、域分离和摘要截取由外部完成；轮常量及线性层必须匹配目标协议。集成依据ready/valid握手和标签，而非固定响应延迟；复位作废在途工作。

### 正确理解性能

理想供数／收数且enable有效时，孤立置换耗时8,562周期；16个在途状态槽使用时，实测稳态窗口内相邻结果间隔536周期。差异来自交织，不能将536周期当作单请求延迟。共享模乘有效发射忙率为105,728次有效发射／120,064稳态窗口周期=88.0597%，不是全芯片活动率。

这些是理想原生接口下已记录的RTL周期测量；按MHz换算的时间／速率不是板级或硅后测量。Design Compiler结果仅适用于文档所列配置、库角和约束，不构成布局布线签核或系统吞吐率结论。条件见 [性能测试](docs/performance.md) 和 [综合结果](docs/synthesis-summary.md)。

## 仓库内容

- **Chisel 设计**：[operators/poseidon2/src/main/scala/zk/poseidon2/](operators/poseidon2/src/main/scala/zk/poseidon2/)，包含算术流水线、线性变换和多上下文调度；其中 [Environment.scala](operators/poseidon2/src/main/scala/zk/poseidon2/Environment.scala) 是验证环境，不属于产品电路。
- **生成好的 SV**：[产品 SV 目录](rtl/v1.1.0/t12-ctx16/)，状态宽度为12个域元素，配置16个交织上下文。顶层是 [GoldilocksPoseidon2](rtl/v1.1.0/t12-ctx16/GoldilocksPoseidon2.sv)，[files.f](rtl/v1.1.0/t12-ctx16/files.f) 列出集成所需的产品文件。
- **参考模型与参数**：[operators/poseidon2/cmodel/](operators/poseidon2/cmodel/) 提供独立 C 参考模型；[operators/poseidon2/reference/](operators/poseidon2/reference/) 保存固定参数与已知答案；[operators/poseidon2/tools/check_reference.py](operators/poseidon2/tools/check_reference.py) 用独立 Python 大整数模型核对参考结果。
- **生成与验证工具**：[tools/emit_sv.py](tools/emit_sv.py) 生成电路；[tools/verify.py](tools/verify.py) 完成参考核对、RTL 仿真和性能统计；[operators/poseidon2/sim/](operators/poseidon2/sim/) 提供 C++ 仿真时钟驱动。
- **设计与集成说明**：[电路功能与数学模型](docs/mathematical-model.md)、[硬件架构](docs/architecture.md)、[原生接口](docs/interface.md)、[工具链说明](docs/toolchain.md) 和 [复现指南](docs/reproduction.md)。
- **性能与综合信息**：[性能测试](docs/performance.md) 分别说明单任务延迟、饱和吞吐率和计算单元有效忙率；[综合结果](docs/synthesis-summary.md) 说明工艺、约束及面积、setup 和逻辑级数结果。

## 应用场景

当系统在 Goldilocks 域上使用12元素状态的 Poseidon2 进行哈希压缩、构建承诺或生成挑战时，可以将其中的完整置换运算交给本模块；轮常量、线性层和轮数需与本模块一致。它提供状态到状态的置换计算，吸收消息、截取摘要等协议封装由集成方完成。以下是部分集成示例，不限于某个证明框架：

- **STARK 证明端的 Merkle 承诺**：对执行轨迹、商多项式或 FRI 各轮数据建立承诺时，把叶子哈希和父节点压缩所调用的置换交给本模块；树的调度与存储由外部负责。Plonky3 的 [Merkle 树实现](https://github.com/Plonky3/Plonky3/blob/main/merkle-tree/src/merkle_tree.rs) 区分了这两类计算。
- **证明端与验证端的 Fiat–Shamir 挑战生成**：在吸收公开输入、承诺和消息后，或输出新的挑战时，执行 duplex sponge 所需的置换；吸收、填充、域分离和输出顺序必须遵循目标协议。参见 Plonky3 的 [DuplexChallenger](https://github.com/Plonky3/Plonky3/blob/main/challenger/src/duplex_challenger.rs)。
- **验证端的 Merkle 路径核验**：重算叶子摘要、沿认证路径逐层压缩，最终与承诺根比较时，可复用相同的置换模块；路径与根比较逻辑由外部负责。

这些应用展示了置换单元的部分集成位置；集成方可以按目标协议组合吸收与输出、填充、域分离、树调度等外围逻辑，将本模块用于更多采用相同置换参数的计算链路。

## 性能指标

典型16上下文配置在理想接口仿真中，单任务延迟为8,562 clk，稳态平均完成间隔为536 clk/任务（按500 MHz换算为17.124 µs和约932,836任务/s），共享模乘有效忙率为88.0597%；详细文档给出指标定义、测试方法与测量结果。 详见[性能测试](docs/performance.md)。

## 数学模型

本电路在模数 $p=2^{64}-2^{32}+1$ 的域上，对12元素状态执行七次幂非线性层、8个完整轮和22个部分轮组成的 Poseidon2 置换，并原样返回32位任务标签；详细文档给出固定常量及各轮的严格定义。 详见[电路功能与数学模型](docs/mathematical-model.md)。

## 快速复现

复现流程涵盖工具准备、SV生成、参考模型比对、功能验证和理想接口周期性能测试。 详见[复现指南](docs/reproduction.md)。

## 综合结果

随包配置在 TSMC 28 nm HPC+ SSG 工艺角的500 MHz、10% setup时钟不确定度约束下无setup违例，映射cell面积约0.142350 mm²，99.7371%的端点最差setup路径逻辑级数严格小于50。 详见[综合结果](docs/synthesis-summary.md)。

## 公开边界

Chisel 设计、SV 生成、参考模型、功能验证和理想接口周期性能测试均提供完整代码，可自行复现。性能测试不加入外部带宽或存储延迟限制，实际系统的供数与集成性能由使用者评估。

很抱歉，出于第三方授权和工程资料保密方面的限制，我们无法随仓库提供已有综合流程使用的工艺库、原始 DC Tcl/SDC、原始报告、映射网表或综合快照。为提供实现参考，我们在 [综合结果](docs/synthesis-summary.md) 中转述了工具版本、工艺条件、约束与关键结果；公开可复现的内容为RTL生成和仿真验证，综合数据对应随包16上下文配置及文中列出的约束。

本项目交付独立置换计算模块；SoC、总线适配、软件驱动、上板流程，以及 sponge、摘要截取、Merkle 树协议和完整 STARK 系统属于集成方的系统设计范围。使用者可按自身需求完成协议封装和系统集成，许可条款见 [MIT 许可证](LICENSE)。

## OpenRTL与定制集成

通过[OpenRTL组织主页](https://github.com/OpenRTL-Lab)、[ZK加速器集合](https://github.com/OpenRTL-Lab/zk-accelerators)及[OpenRTL网站](https://openrtl.top/)查找相关模块与合作联系入口。需要定制数据通路、接口或目标平台适配时可联系维护者；MIT商用权不附加购买服务的要求，不要在公开Issue提交保密规格。

## 发布源码与集成

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。

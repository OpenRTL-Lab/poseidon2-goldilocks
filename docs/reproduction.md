<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/reproduction.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 生成与仿真复现

[English](reproduction.en.md)

本指南提供工具准备、重新生成电路、核对参考模型、验证产品SV和复测周期性能的完整步骤，并介绍生成文件、测试日志、配置参数与常见问题，帮助读者从源码复现功能和性能结果。

## 获取版本快照

[发布 v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-goldilocks.git
cd poseidon2-goldilocks
```

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。

## 环境准备与命令约定

先按 [工具链说明](toolchain.md) 准备工具。安装是执行下列脚本的前提，不是验证命令本身的一部分：

- Python运行脚本和独立大整数参考模型。
- C编译器编译参考模型；其实现需要支持128位无符号整数。
- JDK与Mill编译Chisel并调用电路生成器；首次构建需要解析公开的Scala/Chisel依赖。
- Verilator执行产品lint和仿真；C++编译器及构建工具编译仿真程序。
- firtool由Chisel生成流程调用，版本须与工具链说明一致。

所有命令在仓库根目录执行，所有指定的输出目录都应是新的相对目录，位于 `build/` 下。
脚本不会删除或覆盖已存在的结果；重复运行请换一个目录名称。
例如，第一次使用 `build/verify`，第二次可改为 `build/verify-second`，无需删除旧证据。

完成工具准备后，可以先查看验证脚本的参数说明。这一步只检查Python入口能启动并列出参数，不会生成电路、编译仿真或验证功能：

```bash
python3 tools/verify.py --help
```

下列命令的 `--contexts` 是共享硬件算术的在途置换状态槽数；`--tasks` 是测试请求数，不是硬件复制数量。一任务置换全部12个域元素。

## 一次完成完整链路

下面的命令验证16上下文产品：生成256个任务的参考输入与输出、重新生成SV，并分别测试重新生成的SV和仓库随包SV；同时统计单任务延迟、饱和吞吐率和计算单元有效忙率。`--jobs 8` 仅指定仿真程序编译的并行作业数，不改变硬件配置、供数条件或性能计数：

```bash
python3 tools/verify.py --out build/verify --contexts 16 --tasks 256 --jobs 8
```

任务数是2的幂，最少为上下文数的4倍，最大4096；默认256。
脚本顺序如下：

1. 编译独立C参考模型，生成包含边界输入和确定性随机输入的256个任务、每任务12元素的输入/答案。
2. 用独立Python大整数完整矩阵参考核对C模型、全部向量及已知答案。
3. 用Chisel生成产品和验证环境，提取独立产品依赖闭包并lint。
4. 构建重新生成SV的Verilator仿真，运行六种模式。
5. 去注释比较重新生成与随包SV的模块集合、电路内容；用同一验证环境替换成随包产品SV，再独立构建并运行六种模式。
6. 比较两套SV的逐模式结果，计算延迟、吞吐及有效操作忙率，输出 `build/verify/results.json`。

完整命令退出码0表示所有门禁通过；任一断言、意外退出码、参考差异、模块缺失或生成差异均会使命令失败。
错误负控应检测到故意加入的错误；脚本会将这个子测试的退出1作为预期结果，不会因此令完整命令失败。
查看诊断时打开对应输出目录的日志。日志和编译缓存是本地运行产物，不属于公开包。
临时C++编译目录在成功/异常离开后自动清理；参考向量和测试日志保留在用户指定的输出目录。

### 输出文件与结果判读

| 相对输出位置 | 内容与用途 |
| --- | --- |
| `build/verify/results.json` | 完整成功后生成的结果，包括任务数、上下文数、两套SV的模式结果和周期性能 |
| `build/verify/configuration.json` | 本次生成配置 |
| `build/verify/vectors/input.hex` | 每任务12元素的参考输入 |
| `build/verify/vectors/expected.hex` | 对应C参考输出，另经Python模型核对 |
| `build/verify/rtl/product.f` | 重新生成的独立产品SV文件列表 |
| `build/verify/rtl/simulation.f` | 包含验证环境的仿真文件列表 |
| `build/verify/generated-*.log` | 重新生成SV的lint、编译及逐模式日志 |
| `build/verify/bundled-*.log` | 随包SV的lint、编译及逐模式日志，仅16上下文配置生成 |
| `build/verify/c-compile.log`、`c-reference.log`、`python-reference.log`、`emit.log` | 各前置阶段日志，均位于同一输出目录 |

应检查完整命令退出0、`results.json` 中 `status=0`，以及16上下文配置的 `bundled_matches_generated=true`。每套SV的五个正向模式应 `pass=1`、`mismatches=0`；负控应 `pass=0`、`mismatches=1`。各模式核对的元素数量均应为任务数乘12。

性能字段中，`isolated_latency_min_cycles` / `isolated_latency_max_cycles` 是单任务延迟；`cycles_per_task` 与 `tasks_per_cycle` 是稳态任务吞吐率的两种表达；`mul_busy_fraction` 等字段统计有效计算操作，而非仅流水线中存在在途数据。具体窗口、计算方法和既有测量值见 [性能测试](performance.md)。

## 六种模式

以下数量对应上述256任务命令；若改变任务数，元素数量随之变为任务数乘12。

| 模式 | 检查内容 | 预期 |
| --- | --- | --- |
| ideal | 始终供数/收数，允许启动 | 3072元素，0错 |
| protocol | 请求空隙、响应背压和使能变化；握手期间数据稳定性 | 3072元素，0错 |
| reset | 运行中复位，作废在途事务并重新提交全部任务 | 3072元素，0错 |
| negative | 故意翻转一个参考结果元素的一位 | 检测到1个错误；该子进程退出1 |
| isolated | 一次只允许一个任务在途 | 3072元素，0错；用于单任务延迟 |
| disabled-buffer | 前64周期禁止启动，确认只能缓冲2任务、未完成任务 | 3072元素，0错 |

协议模式只是功能正确性门禁，不做外部带宽、延迟或背压敏感性扫描。
性能只取 ideal 和 isolated；环境ROM是理想测试供数模型，不是产品的外部存储实现。

## 单独生成SV

如果只需要生成电路和产品文件清单，可运行下面的命令。它会生成参考向量并用Python核对，随后调用Chisel生成产品及验证环境；不会运行Verilator完整回归，也不会产生完整性能结果：

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --tasks 256
```

`build/emit/rtl/product.f` 为独立产品SV的相对路径列表；`build/emit/rtl/simulation.f` 还包含验证环境与验证层。
只集成产品时使用 `product.f`，不要把验证ROM和时钟驱动一起综合。
`--contexts` 改变硬件中的交织上下文数；`--tasks` 控制验证环境的任务与向量数量，不改变置换的数学定义。

如果只想检查仓库随包产品SV的语法和结构，而不重新生成或执行输入输出比对，可直接运行产品lint。这不等于完整功能或性能验证：

```bash
verilator --lint-only --assert --top-module GoldilocksPoseidon2 -f rtl/v1.1.0/t12-ctx16/files.f
```

## 32上下文配置

要生成并验证32上下文配置，使用下面的命令。它完成参考核对、产品lint、六模式功能验证和理想接口周期性能统计；因为仓库随包的是16上下文SV，所以不进行跨配置的随包比较：

```bash
python3 tools/verify.py --out build/verify-ctx32 --contexts 32 --tasks 256 --jobs 8
```

32上下文配置通过256任务功能验证；500 MHz综合摘要只针对16上下文，不外推到32上下文。

## 常见失败与复测注意事项

- **工具找不到或版本不兼容**：按工具链说明检查所需命令与依赖；若Mill启动器另有名称，可使用文档说明的 `MILL` 环境变量。
- **首次构建依赖下载失败**：检查访问公开依赖源的网络及用户自选缓存的读写权限；本仓库不绑定代理或缓存目录。
- **输出目录已存在**：换用新的 `build/` 相对目录。不要把旧目录里的结果当作本次验证成功的证据。
- **参数不合法**：上下文数只能16或32；任务数须为2的幂，至少为上下文数的4倍、最多4096；`--jobs` 在1至64之间。
- **参考、lint或仿真失败**：先查看对应阶段日志。完整命令失败时可能已有部分输出，但不能视为完整验证通过。
- **只生成成功**：emit命令退出0仅说明生成及前置参考核对成功；要证明功能和周期性能，应执行完整验证命令。

延迟、吞吐率和忙率以RTL仿真周期计量；时间和任务/秒按指定频率换算。随包16上下文配置的综合条件与测量数据见 [综合结果](synthesis-summary.md)。

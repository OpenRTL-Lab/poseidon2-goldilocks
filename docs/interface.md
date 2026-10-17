<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/interface.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 原生接口

[English](interface.en.md)

本文件定义 `GoldilocksPoseidon2` 的单时钟原生握手接口、12元素请求与响应、32位标签，以及背压、使能和复位语义，供集成方直接连接任务输入与结果输出；所有握手在时钟上升沿发生。

一次请求是一遍完整的12元素置换。每个输入字须为无符号规范值 0 ≤ x < p，其中 p = 2^64 - 2^32 + 1；输出表示相同，不定义非规范输入的行为。上下文是保存一项在途置换状态的槽位，各槽位共享算术流水线，不是各一套置换核。随包产品有16个槽位，输入队列与这些槽位是分开的。精确固定置换见 [电路功能与数学模型](mathematical-model.md)。

| 信号 | 方向 | 定义 |
| --- | --- | --- |
| `clock` | 输入 | 工作时钟 |
| `reset` | 输入 | 同步、高有效复位 |
| `io_enable` | 输入 | 允许输入队列中的任务进入空闲上下文 |
| `io_request_valid` | 输入 | 请求有效 |
| `io_request_ready` | 输出 | 输入队列可接收 |
| `io_request_bits_tag` | 输入，32位 | 任务标签，输出原样携带 |
| `io_request_bits_words_0` 到 `_11` | 输入，每个64位 | 一次完整置换的12个域元素 |
| `io_response_valid` | 输出 | 完整结果有效 |
| `io_response_ready` | 输入 | 下游允许接收结果 |
| `io_response_bits_tag` | 输出，32位 | 对应任务标签 |
| `io_response_bits_words_0` 到 `_11` | 输出，每个64位 | 12个置换结果元素 |

一次握手当且仅当对应 `valid && ready` 为真。没有部分元素传输，也没有额外的启动脉冲或 SoC 控制寄存器。
请求遇到背压时，发送者必须保持 `valid` 和整组数据、标签稳定；响应遇到背压时，电路同样保持稳定。
外部不得靠假设固定延迟跳过握手；用标签关联任务，复位后所有在途事务作废，由调用方重新提交。
接口不要求标签连续，也不要求唯一；测试环境使用连续唯一标签便于检查漏包/重复包。

`io_enable=0` 只禁止从输入队列启动新任务，不清空队列、不撤回已经接受的请求、不冻结已启动计算。
因此禁启动期间输入队列仍可接受最多2个请求；恢复使能后继续处理。
输出持续背压最终会使调度和输入准入停顿，不能保证无限接受任务。
性能测试仅使用持续供数、持续收数和 `io_enable=1`，协议测试中的背压不计入性能结果。

Chisel 类中还声明了辅助 `busy` 状态；本版本从验证层提取的固定产品SV没有该端口，因为未使用的辅助输出被生成工具裁剪。
随包SV及脚本复现的对外契约以本表为准，集成时不应依赖 `io_busy`。
直接从 Chisel 类生成并保留该辅助端口时，产品接口和综合输入会随之改变，应使用该生成产物重新验证和综合。

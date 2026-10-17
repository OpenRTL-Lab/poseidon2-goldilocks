<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/publication-checklist.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 发布内容与检查

[English](publication-checklist.en.md)

本版本提供独立 Goldilocks Poseidon2 置换、可复现验证、双语文档和 MIT 许可。

## 设计与验证

- 随包产品采用16上下文；生成器还支持通过功能验证的32上下文配置。综合数据仅适用于文档中的16上下文配置。
- 仓库包含公开 Chisel、产品 SV、C/Python 参考和验证脚本。重新生成与随包 SV 通过文档所列六种模式，测量结果一致。
- 原生边界是状态到状态的置换，不是 sponge、字节哈希、Merkle 引擎或完整证明系统。
- 复现文档说明命令、成功判据与已测配置；综合摘要说明工具、工艺角、约束、WNS/TNS、面积与逻辑深度定义。

## 文档与下载

默认 README.md 为英文，README.zh-CN.md 为独立中文全文；其他自编 Markdown 也各有独立译文，并通过语言链接互相跳转。Release 提供硬件源码包及双语设计、证据和下载说明。产品文件清单不包含验证环境。

## 公开文件检查

```bash
python3 tools/public_audit.py
```

审查器检查文件白名单、产品列表、双语配对、相对链接、凭证样式与披露边界。载荷不包含机器路径、执行日志、内部记录、工具缓存或受授权限制的综合资料；自动检查不代替许可与信息披露审查。

## 许可与范围

根 LICENSE 及适用的第三方 MIT 声明定义再分发条件。数学常量及必要归属声明是交付物的一部分。仓库不分发商业工具、工艺库、原始综合脚本/报告、映射网表或综合快照。仿真和综合不等于上板或硅后认证。

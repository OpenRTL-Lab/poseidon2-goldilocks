<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/mathematical-model.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 电路功能与数学模型

[English](mathematical-model.en.md)

本文件严格定义12元素 Goldilocks Poseidon2 置换的输入输出、有限域、七次幂非线性层、外部与内部线性层、固定常量和轮次递推，并说明参数来源与集成匹配条件；一次任务输出完整的12元素置换结果及原样返回的32位标签。

## 输入、输出与有限域

一次任务执行完整的12元素置换，而不是单个轮、单次哈希吸收或摘要截取。设

$$
p=2^{64}-2^{32}+1,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z.
$$

输入与输出分别为列向量 $\mathbf x,\mathbf y\in\mathbb F_p^{12}$。接口每个元素使用64位无符号整数表示，调用方须保证输入在 $[0,p)$，输出也采用该规范表示；不定义非规范输入的行为。32位任务标签 $\tau$ 只用于关联请求与响应，不参与数学运算：

$$
(\mathbf x,\tau)\longmapsto\bigl(\Pi(\mathbf x),\tau\bigr),
\qquad 0\leq\tau<2^{32}.
$$

以下所有加法、乘法、幂和矩阵运算均在 $\mathbb F_p$ 中进行，索引从0开始。接口握手、复位和使能语义见 [原生接口](interface.md)。

## 外部与内部线性变换

外部变换记作 $E(\mathbf z)=M_E\mathbf z$，其中

$$
B=\begin{bmatrix}
2&3&1&1\\
1&2&3&1\\
1&1&2&3\\
3&1&1&2
\end{bmatrix},\qquad
M_E=\begin{bmatrix}
2B&B&B\\
B&2B&B\\
B&B&2B
\end{bmatrix}.
$$

三个块依次对应元素 $0\ldots3$、$4\ldots7$、$8\ldots11$。内部变换记作 $I$：

$$
I(\mathbf z)_i=\sum_{j=0}^{11}z_j+d_i z_i,
\qquad 0\leq i<12,
$$

$$
\mathbf d=\left(-2,1,2,2^{-1},3,4,-2^{-1},-3,-4,2^{-2},-2^{-2},2^{-3}\right).
$$

这里 $2^{-k}$ 表示 $2^k$ 在 $\mathbb F_p$ 中的乘法逆元，不是整数移位或实数除法。内部矩阵等价于全1矩阵加 $\mathrm{diag}(\mathbf d)$；因此其主对角元素是 $1+d_i$，不是 $d_i$。

## 非线性变换、轮常量与完整置换

S-box 为 $s(a)=a^7$。完整轮对全部12个元素使用 S-box，部分轮只对元素0使用 S-box。定义

$$
S(\mathbf z)_i=z_i^7,\qquad
P_h(\mathbf z)=\bigl((z_0+h)^7,z_1,\ldots,z_{11}\bigr)^{\mathsf T}.
$$

精确的轮常量由本仓库 [parameters.json](../operators/poseidon2/reference/parameters.json) 固定，而非运行时生成或从上游动态读取：

- $\mathbf a_r$：`GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL` 的第 $12r+i$ 项是 $a_{r,i}$，$0\leq r<4$、$0\leq i<12$。
- $\mathbf b_r$：`GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_FINAL` 的第 $12r+i$ 项是 $b_{r,i}$，索引范围同上。
- $h_r$：`GOLDILOCKS_POSEIDON2_RC_12_INTERNAL` 的第 $r$ 项，$0\leq r<22$。
- $d_i$：`MATRIX_DIAG_12_GOLDILOCKS` 的第 $i$ 项，是上式对应系数的规范域表示。

上述文件的数值字符串均按十六进制解释。完整置换采用以下递推，运算顺序不可交换：

$$
\begin{aligned}
\mathbf u_0&=E(\mathbf x),\\
\mathbf u_{r+1}&=E\bigl(S(\mathbf u_r+\mathbf a_r)\bigr), &&0\leq r<4,\\
\mathbf v_0&=\mathbf u_4,\\
\mathbf v_{r+1}&=I\bigl(P_{h_r}(\mathbf v_r)\bigr), &&0\leq r<22,\\
\mathbf w_0&=\mathbf v_{22},\\
\mathbf w_{r+1}&=E\bigl(S(\mathbf w_r+\mathbf b_r)\bigr), &&0\leq r<4,\\
\Pi(\mathbf x)&=\mathbf w_4.
\end{aligned}
$$

即先进行一次初始外部线性变换，再依次执行4个完整轮、22个部分轮、4个完整轮；总计8个完整轮和22个部分轮。输出全部12个元素。

此固定参数集的轮常量和内部系数对应 Plonky3 的 [默认 Goldilocks 12元素置换](https://github.com/Plonky3/Plonky3/blob/main/goldilocks/src/poseidon2.rs)，外部线性层采用其 [MDSMat4](https://github.com/Plonky3/Plonky3/blob/main/poseidon2/src/external.rs)。电路的精确定义以本仓库冻结的参数文件和上述公式为准；仅有“Poseidon2”名称相同不足以判定兼容。采用 BabyBear、其他状态宽度、其他矩阵或其他轮常量的配置，不能直接由此电路替代。

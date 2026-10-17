<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Hardware Accelerator
Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
File: docs/mathematical-model.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Circuit Function and Mathematical Model

[简体中文](mathematical-model.md)

This document precisely defines the inputs, outputs, finite field, seventh-power nonlinear layer, external and internal linear layers, fixed constants, and round recurrence of the 12-element Goldilocks Poseidon2 permutation, including its parameter sources and integration-matching conditions; each task returns all 12 permuted elements and the original 32-bit tag.

## Inputs, outputs, and finite field

One task executes a complete 12-element permutation, not a single round, hash absorption, or digest extraction. Let

$$
p=2^{64}-2^{32}+1,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z.
$$

Inputs and outputs are column vectors $\mathbf x,\mathbf y\in\mathbb F_p^{12}$. Each interface element is a 64-bit unsigned integer. The caller must supply inputs in $[0,p)$; outputs use the same canonical representation. Behavior for noncanonical inputs is not defined. The 32-bit task tag $\tau$ associates requests and responses and does not participate in the arithmetic:

$$
(\mathbf x,\tau)\longmapsto\bigl(\Pi(\mathbf x),\tau\bigr),
\qquad 0\leq\tau<2^{32}.
$$

All additions, multiplications, powers, and matrix operations below are in $\mathbb F_p$, with zero-based indices. See [Native interface](interface.en.md) for handshake, reset, and enable semantics.

## External and internal linear transformations

Write the external transformation as $E(\mathbf z)=M_E\mathbf z$, where

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

The three blocks correspond to elements $0\ldots3$, $4\ldots7$, and $8\ldots11$. Write the internal transformation as $I$:

$$
I(\mathbf z)_i=\sum_{j=0}^{11}z_j+d_i z_i,
\qquad 0\leq i<12,
$$

$$
\mathbf d=\left(-2,1,2,2^{-1},3,4,-2^{-1},-3,-4,2^{-2},-2^{-2},2^{-3}\right).
$$

Here $2^{-k}$ is the multiplicative inverse of $2^k$ in $\mathbb F_p$, not an integer shift or real-number division. The internal matrix is the all-ones matrix plus $\mathrm{diag}(\mathbf d)$, so its main-diagonal entries are $1+d_i$, not $d_i$.

## Nonlinear transformations, round constants, and complete permutation

The S-box is $s(a)=a^7$. Full rounds apply it to all 12 elements; partial rounds apply it only to element 0. Define

$$
S(\mathbf z)_i=z_i^7,\qquad
P_h(\mathbf z)=\bigl((z_0+h)^7,z_1,\ldots,z_{11}\bigr)^{\mathsf T}.
$$

Exact round constants are fixed by this repository's [parameters.json](../operators/poseidon2/reference/parameters.json), not generated at runtime or fetched dynamically from upstream:

- $\mathbf a_r$: entry $12r+i$ of `GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL` is $a_{r,i}$, for $0\leq r<4$ and $0\leq i<12$.
- $\mathbf b_r$: entry $12r+i$ of `GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_FINAL` is $b_{r,i}$, with the same index ranges.
- $h_r$: entry $r$ of `GOLDILOCKS_POSEIDON2_RC_12_INTERNAL`, for $0\leq r<22$.
- $d_i$: entry $i$ of `MATRIX_DIAG_12_GOLDILOCKS`, the canonical field representation of the coefficient above.

The numeric strings in that file are hexadecimal. The complete permutation follows this recurrence; the operation order is fixed:

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

Thus, an initial external linear transformation is followed by 4 full rounds, 22 partial rounds, and 4 full rounds, totaling 8 full and 22 partial rounds. All 12 elements are returned.

The round constants and internal coefficients of this fixed parameter set match Plonky3's [Default 12-element Goldilocks permutation](https://github.com/Plonky3/Plonky3/blob/main/goldilocks/src/poseidon2.rs), and the external linear layer uses its [MDSMat4](https://github.com/Plonky3/Plonky3/blob/main/poseidon2/src/external.rs). The frozen parameter file and formulas above define this circuit precisely; sharing the name “Poseidon2” alone does not establish compatibility. Configurations using BabyBear, another state width, different matrices, or different round constants cannot be directly replaced by this circuit.

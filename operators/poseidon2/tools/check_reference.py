#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Hardware Accelerator
# Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
# File: operators/poseidon2/tools/check_reference.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""独立大整数矩阵参考，验证上游KAT、参数转写和全部C向量。"""
import json
from pathlib import Path
import sys
OP = Path(__file__).resolve().parents[1]
PARAM = json.loads((OP / 'reference/parameters.json').read_text())
P = 2**64 - 2**32 + 1
rc0 = [int(x,16) for x in PARAM['GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL']]
rc1 = [int(x,16) for x in PARAM['GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_FINAL']]
rp = [int(x,16) for x in PARAM['GOLDILOCKS_POSEIDON2_RC_12_INTERNAL']]
diag = [int(x,16) for x in PARAM['MATRIX_DIAG_12_GOLDILOCKS']]
M = [[2,3,1,1],[1,2,3,1],[1,1,2,3],[3,1,1,2]]
def external(x):
    # 构建完整12×12矩阵，和C分块循环、RTL因式分解不同。
    return [sum((2 if i//4==j//4 else 1)*M[i%4][j%4]*x[j]
                for j in range(12)) % P for i in range(12)]
def permutation(x):
    x=external(x)
    for c in (rc0[i:i+12] for i in range(0,48,12)):
        x=external([pow(a+b,7,P) for a,b in zip(x,c)])
    for c in rp:
        x[0]=pow(x[0]+c,7,P);s=sum(x)
        x=[(s+d*a)%P for a,d in zip(x,diag)]
    for c in (rc1[i:i+12] for i in range(0,48,12)):
        x=external([pow(a+b,7,P) for a,b in zip(x,c)])
    return x
assert permutation(list(range(12))) == [int(x,16) for x in PARAM['kat']]
if len(sys.argv)>1:
    v=Path(sys.argv[1])
    inputs=[int(x,16) for x in (v/'input.hex').read_text().split()]
    expected=[int(x,16) for x in (v/'expected.hex').read_text().split()]
    assert len(inputs)==len(expected) and len(inputs)%12==0
    for i in range(0,len(inputs),12):
        assert permutation(inputs[i:i+12])==expected[i:i+12],i//12
    print(f"PYTHON_REFERENCE tasks={len(inputs)//12} words={len(inputs)} pass=1")
else:
    print("PYTHON_KAT pass=1")

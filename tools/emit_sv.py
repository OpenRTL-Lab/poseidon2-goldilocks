#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Hardware Accelerator
# Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
# File: tools/emit_sv.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""便携生成入口：生成参考向量、Chisel验证环境和独立产品文件清单。"""
from source_headers import stamp_generated
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
OP = Path('operators/poseidon2')


def relative_build(value):
    path = Path(value)
    if path.is_absolute() or '..' in path.parts or not path.parts or path.parts[0] != 'build':
        raise ValueError('输出必须是 build 内的相对目录，不允许覆盖已有目录')
    return path


def run(command, output, label, expected=0):
    with (output / (label + '.log')).open('x') as log:
        result = subprocess.run([str(x) for x in command], cwd=ROOT,
                                stdout=log, stderr=subprocess.STDOUT)
    if result.returncode != expected:
        raise RuntimeError(f'{label} 失败，请检查本次输出目录的对应日志')


def code(text):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)


def product_closure(rtl):
    stamp_generated(rtl)
    definitions, texts = {}, {}
    for path in sorted(rtl.glob('*.sv')):
        # 只去掉源码定位注释，不改电路、参数或必要版权声明。
        text = re.sub(r'[ \t]*//[^\n]*\.scala:\d+:\d+[^\n]*', '', path.read_text())
        path.write_text(text)
        texts[path] = code(text)
        for module in re.findall(r'\bmodule\s+(\w+)', texts[path]):
            assert module not in definitions, '模块重复'
            definitions[module] = path
    selected, pending = set(), ['GoldilocksPoseidon2']
    while pending:
        path = definitions[pending.pop()]
        if path in selected:
            continue
        selected.add(path)
        instances = set(re.findall(
            r'^[ \t]*(\w+)\b[ \t]+(?:#\s*\([^;]*?\)\s*)?\w+\s*\(',
            texts[path], re.M | re.S))
        pending.extend(instances & definitions.keys())
    for path in selected:
        stripped = re.sub(r'"(?:[^"\\]|\\.)*"', '""', texts[path])
        assert not re.search(r'\$readmem|%|\b(?:DSP48\w*|RAMB\w*|URAM\w*|BUFG\w*|IOBUF)\b', stripped)
        assert 'SimTop' not in stripped
    return sorted(selected)


def simulation_list(rtl, output, bundled=False):
    sources = sorted(rtl.rglob('*.sv'))
    if bundled:
        generated_product = set(product_closure(rtl))
        sources = [p for p in sources if p not in generated_product]
        sources += sorted(Path('rtl/v1.1.0/t12-ctx16').glob('*.sv'))
    includes = sorted({p.parent for p in sources})
    output.write_text(''.join('+incdir+' + str(p) + '\n' for p in includes)
                      + ''.join(str(p) + '\n' for p in sources))


def generate(output, contexts=16, tasks=256):
    os.chdir(ROOT)
    output = relative_build(output)
    if output.exists():
        raise ValueError('输出目录已存在；请换一个新目录，不会自动删除旧结果')
    if contexts not in (16, 32) or not (4*contexts <= tasks <= 4096) or tasks & (tasks-1):
        raise ValueError('上下文数仅16/32；任务数为2的幂，介于4倍上下文数与4096之间')
    output.mkdir(parents=True)
    vectors = output / 'vectors'
    vectors.mkdir()
    run([os.environ.get('CC', 'cc'), '-std=c11', '-O2', '-Wall', '-Wextra', '-Werror',
         OP / 'cmodel/reference.c', '-o', output / 'reference'], output, 'c-compile')
    run([output / 'reference', vectors, tasks], output, 'c-reference')
    run([sys.executable, OP / 'tools/check_reference.py', vectors], output, 'python-reference')
    rtl = output / 'rtl'
    run(shlex.split(os.environ.get('MILL', 'mill')) + ['-i', 'poseidon2.tool.runMain', 'zk.poseidon2.Emit',
         rtl, contexts, vectors, tasks], output, 'emit')
    product = product_closure(rtl)
    (rtl / 'product.f').write_text(''.join(str(p) + '\n' for p in product))
    simulation_list(rtl, rtl / 'simulation.f')
    (output / 'configuration.json').write_text(json.dumps(
        {'version': '1.1.0', 'width': 12, 'contexts': contexts, 'tasks': tasks}, indent=2) + '\n')
    return rtl


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', default='build/emit')
    parser.add_argument('--contexts', type=int, default=16)
    parser.add_argument('--tasks', type=int, default=256)
    args = parser.parse_args()
    generate(args.out, args.contexts, args.tasks)
    print('生成通过；产品文件见输出目录 rtl/product.f，验证环境不属于产品')

#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Hardware Accelerator
# Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
# File: tools/verify.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""独立C/Python参考、重新生成、随包SV复验、协议负控与理想性能。"""
import argparse
from collections import Counter
import json
import os
from pathlib import Path
import re
import tempfile

from emit_sv import ROOT, OP, code, generate, product_closure, run, simulation_list

MODES = [(0, 'ideal'), (1, 'protocol'), (2, 'reset'),
         (3, 'negative'), (4, 'isolated'), (5, 'disabled-buffer')]


def entries(log, prefix):
    return [dict((key, int(value)) for key, value in re.findall(r'(\w+)=\s*(\d+)', line))
            for line in re.findall('^' + prefix + r' (.*)$', log, re.M)]


def performance(log, isolated_log, contexts, tasks):
    accepted, completed = entries(log, 'ACCEPT'), entries(log, 'COMPLETE')
    assert len(accepted) == len(completed) == tasks
    assert [x['tag'] for x in completed] == list(range(tasks))
    assert all(x['errors'] == 0 for x in completed)
    work = Counter((int(tag), int(kind)) for tag, kind in re.findall(
        r'^WORK tag=\s*(\d+) kind=\s*(\d+)', log, re.M))
    for tag in range(tasks):
        assert all(work[tag, kind] == 118 for kind in (3, 4, 5, 6))
        assert work[tag, 1] == work[tag, 2] == 9
        assert work[tag, 7] == work[tag, 8] == 22
    first, last = completed[contexts], completed[tasks-contexts]
    duration, intervals = last['cycle'] - first['cycle'], last['tag'] - first['tag']
    def operations(kinds):
        return sum(v for (tag, kind), v in work.items()
                   if first['tag'] <= tag < last['tag'] and kind in kinds)
    iso_a, iso_c = entries(isolated_log, 'ACCEPT'), entries(isolated_log, 'COMPLETE')
    assert len(iso_a) == len(iso_c) == tasks
    assert all(iso_a[i]['cycle'] > iso_c[i-1]['cycle'] for i in range(1, tasks))
    return {
        'isolated_latency_min_cycles': min(x['latency'] for x in iso_c),
        'isolated_latency_max_cycles': max(x['latency'] for x in iso_c),
        'saturated_latency_min_cycles': min(x['latency'] for x in completed),
        'saturated_latency_max_cycles': max(x['latency'] for x in completed),
        'steady_first_tag': first['tag'], 'steady_last_tag': last['tag'],
        'steady_cycles': duration, 'steady_task_intervals': intervals,
        'cycles_per_task': duration / intervals, 'tasks_per_cycle': intervals / duration,
        'mul_valid_operations': operations((3, 4, 5, 6)),
        'mul_busy_fraction': operations((3, 4, 5, 6)) / duration,
        'external_mat4_busy_fraction': operations((1,)) / duration,
        'external_sum_busy_fraction': operations((2,)) / duration,
        'internal_sum_busy_fraction': operations((7,)) / duration,
        'internal_diagonal_busy_fraction': operations((8,)) / duration,
    }


def verify_binary(rtl, output, jobs, bundled=False):
    label = 'bundled' if bundled else 'generated'
    filelist = output / (label + '.f')
    simulation_list(rtl, filelist, bundled)
    # 编译中间文件可包含运行机器路径，放入临时目录，不属于公开载荷。
    with tempfile.TemporaryDirectory(prefix='poseidon2-') as directory:
        obj = Path(directory)
        run(['verilator', '-O3', '--assert', '--cc', '--exe', '--build', '-j', jobs,
             '-DENABLE_INITIAL_MEM_', '--top-module', 'Poseidon2SimTop', '-Mdir', obj,
             '-CFLAGS', '-O2 -std=c++17', '-f', filelist, (ROOT / OP / 'sim/driver.cpp')],
            output, label + '-compile')
        results = {}
        for mode, name in MODES:
            run([obj / 'VPoseidon2SimTop', mode], output, label + '-' + name,
                1 if mode == 3 else 0)
            log = (output / (label + '-' + name + '.log')).read_text()
            rows = entries(log, 'RESULT')
            assert len(rows) == 1
            result = rows[0]
            assert result['mismatches'] == (1 if mode == 3 else 0)
            assert result['pass'] == (0 if mode == 3 else 1)
            results[name] = result
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', default='build/verify')
    parser.add_argument('--contexts', type=int, default=16)
    parser.add_argument('--tasks', type=int, default=256)
    parser.add_argument('--jobs', type=int, default=2)
    args = parser.parse_args()
    assert 1 <= args.jobs <= 64
    os.environ['CCACHE_DISABLE'] = '1'
    rtl = generate(args.out, args.contexts, args.tasks)
    output = Path(args.out)
    run(['verilator', '--lint-only', '--assert', '--top-module', 'GoldilocksPoseidon2',
         '-f', rtl / 'product.f'], output, 'generated-lint')
    generated = verify_binary(rtl, output, args.jobs)
    for result in generated.values():
        assert result['checked'] == 12 * args.tasks
    report = {'version': '1.1.0', 'contexts': args.contexts, 'tasks': args.tasks,
              'ideal_performance': True, 'dc_run': False, 'generated_tests': generated}
    if args.contexts == 16:
        bundled = Path('rtl/v1.1.0/t12-ctx16')
        originals = {p.name: p for p in bundled.glob('*.sv')}
        newly_generated = {p.name: p for p in product_closure(rtl)}
        assert originals.keys() == newly_generated.keys(), '随包模块集合不一致'
        for name in originals:
            a = re.sub(r'\s+', '', code(originals[name].read_text()))
            b = re.sub(r'\s+', '', code(newly_generated[name].read_text()))
            assert a == b, '重新生成的产品电路与随包产品不一致：' + name
        run(['verilator', '--lint-only', '--assert', '--top-module', 'GoldilocksPoseidon2',
             '-f', bundled / 'files.f'], output, 'bundled-lint')
        report['bundled_tests'] = verify_binary(rtl, output, args.jobs, True)
        assert report['bundled_tests'] == generated, '随包与重新生成的测试结果不一致'
        report['bundled_matches_generated'] = True
    else:
        report['bundled_matches_generated'] = None
    report['performance'] = performance((output / 'generated-ideal.log').read_text(),
        (output / 'generated-isolated.log').read_text(), args.contexts, args.tasks)
    report['status'] = 0
    (output / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()

/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/sim/driver.cpp
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include "VPoseidon2SimTop.h"
#include "verilated.h"
#include <cstdlib>
#include <iostream>
// 仅时钟、复位、测试模式和结果，不访问DUT数据接口。
int main(int argc,char**argv){
  VerilatedContext context;context.commandArgs(argc,argv);
  VPoseidon2SimTop top{&context};
  auto tick=[&](){top.clock=0;top.eval();context.timeInc(1);top.clock=1;top.eval();context.timeInc(1);};
  top.reset=1;top.io_trigger=0;top.io_mode=argc>1?std::atoi(argv[1]):0;
  for(int i=0;i<8;i++)tick();
  top.reset=0;tick();top.io_trigger=1;tick();top.io_trigger=0;
  unsigned ticks=0;
  while(!top.io_done&&!context.gotFinish()&&ticks<10100000){tick();ticks++;}
  bool ok=top.io_done&&top.io_pass&&top.io_mismatches==0;
  std::cout<<"RESULT pass="<<ok<<" checked="<<top.io_checked<<" mismatches="<<top.io_mismatches
    <<" cycles="<<top.io_cycles<<'\n';
  top.final();return ok?0:1;
}

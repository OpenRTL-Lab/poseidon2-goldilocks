/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/src/main/scala/zk/poseidon2/Poseidon2.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.poseidon2
import chisel3._
import chisel3.util._

case class PoseidonConfig(contexts: Int = 16) {
  require(Set(16,32).contains(contexts))
  val indexBits = log2Ceil(contexts)
}
class NativeState extends Bundle {
  val tag = UInt(32.W); val words = Vec(12,UInt(64.W))
}

/** 轮转交织上下文；算术反馈在下次访问前完成，接口均无旁路。 */
class GoldilocksPoseidon2(val cfg: PoseidonConfig) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool()); val request = Flipped(Decoupled(new NativeState))
    val response = Decoupled(new NativeState); val busy = Output(Bool())
  })
  val Idle=0.U(4.W); val ExtA=1.U(4.W); val ExtB=2.U(4.W)
  val Square=3.U(4.W); val Cube=4.U(4.W); val Fourth=5.U(4.W)
  val Seventh=6.U(4.W); val IntA=7.U(4.W); val IntB=8.U(4.W); val Done=9.U(4.W)
  val slot = RegInit(0.U(cfg.indexBits.W))
  val phase = RegInit(VecInit(Seq.fill(cfg.contexts)(Idle)))
  val states = Reg(Vec(cfg.contexts,Vec(12,UInt(64.W))))
  val tags = Reg(Vec(cfg.contexts,UInt(32.W)))
  val rounds = Reg(Vec(cfg.contexts,UInt(5.W))); val word = Reg(Vec(cfg.contexts,UInt(4.W)))
  val initial = Reg(Vec(cfg.contexts,Bool()))
  val base = Reg(Vec(cfg.contexts,UInt(64.W))); val x2 = Reg(Vec(cfg.contexts,UInt(64.W)))
  val x3 = Reg(Vec(cfg.contexts,UInt(64.W))); val x4 = Reg(Vec(cfg.contexts,UInt(64.W)))
  val total = Reg(Vec(cfg.contexts,UInt(64.W)))
  val sel = (0 until cfg.contexts).map(c => slot===c.U)
  def pick[T <: Data](bank: Vec[T]): T = Mux1H(sel,bank.toSeq)
  val ph = pick(phase)
  val input = Module(new Queue(new NativeState,2,pipe=false,flow=false))
  val output = Module(new Queue(new NativeState,2,pipe=false,flow=false))
  input.io.enq <> io.request
  io.response <> output.io.deq
  input.io.deq.ready := io.enable && ph===Idle
  output.io.enq.valid := ph===Done
  output.io.enq.bits.tag := pick(tags)
  output.io.enq.bits.words := pick(states)
  io.busy := phase.map(_=/=Idle).reduce(_||_) || input.io.deq.valid || output.io.deq.valid
  when(!(ph===Done && !output.io.enq.ready)) { slot:=slot+1.U }

  // 调度读取与所有算术之间的明确寄存边界。
  val rdState = RegNext(pick(states)); val rdPhase = RegNext(ph)
  val rdWord = RegNext(pick(word)); val rdRound = RegNext(pick(rounds))
  val sbox = Module(new PoseidonSbox)
  sbox.io.words:=rdState; sbox.io.phase:=rdPhase; sbox.io.word:=rdWord; sbox.io.round:=rdRound
  sbox.io.base:=RegNext(pick(base)); sbox.io.x2:=RegNext(pick(x2))
  sbox.io.x3:=RegNext(pick(x3)); sbox.io.x4:=RegNext(pick(x4))
  val linear = Module(new PoseidonLinear)
  linear.io.words:=rdState; linear.io.phase:=rdPhase; linear.io.total:=RegNext(pick(total))
  val issue = ph>=Square && ph<=Seventh
  val linearIssue = ph===ExtA || ph===ExtB || ph===IntA || ph===IntB
  val resultValid = ShiftRegister(issue,15,false.B,true.B)
  val resultSlot = ShiftRegister(slot,15); val resultWord=ShiftRegister(pick(word),15)
  val resultPhase=ShiftRegister(ph,15)
  val linearValid=ShiftRegister(linearIssue,10,false.B,true.B)
  val linearSlot=ShiftRegister(slot,10); val linearPhase=ShiftRegister(ph,10)
  def advance(c: Int): Unit = {
    when(rounds(c)===29.U) { phase(c):=Done }
    .otherwise { rounds(c):=rounds(c)+1.U; word(c):=0.U; phase(c):=Square }
  }
  for(c <- 0 until cfg.contexts) {
    when(sel(c)) {
      switch(ph) {
        is(Idle) { when(input.io.deq.fire) {
          for(i <- 0 until 12) {
            assert(input.io.deq.bits.words(i)<Parameters.P.U(64.W))
            states(c)(i):=input.io.deq.bits.words(i)
          }
          tags(c):=input.io.deq.bits.tag; rounds(c):=0.U; word(c):=0.U
          initial(c):=true.B; phase(c):=ExtA
        }}
        is(ExtA) { phase(c):=ExtB }
        is(ExtB) { when(initial(c)) { initial(c):=false.B; phase(c):=Square }.otherwise { advance(c) } }
        is(Square) { phase(c):=Cube }
        is(Cube) { phase(c):=Fourth }
        is(Fourth) { phase(c):=Seventh }
        is(Seventh) {
          val full=rounds(c)<4.U || rounds(c)>=26.U
          when(full && word(c)=/=11.U) { word(c):=word(c)+1.U; phase(c):=Square }
          .otherwise { phase(c):=Mux(full,ExtA,IntA) }
        }
        is(IntA) { phase(c):=IntB }
        is(IntB) { advance(c) }
        is(Done) { when(output.io.enq.fire) { phase(c):=Idle } }
      }
    }
    when(resultValid && resultSlot===c.U) {
      assert(slot=/=c.U,"feedback must precede context reuse")
      switch(resultPhase) {
        is(Square) { base(c):=sbox.io.normalized; x2(c):=sbox.io.product }
        is(Cube) { x3(c):=sbox.io.product }
        is(Fourth) { x4(c):=sbox.io.product }
        is(Seventh) { states(c)(resultWord):=sbox.io.product }
      }
    }
    when(linearValid && linearSlot===c.U) {
      assert(slot=/=c.U,"linear feedback must precede context reuse")
      when(linearPhase===IntA) { total(c):=linear.io.result(0) }
      .otherwise { states(c):=linear.io.result }
    }
  }
  // 仅仿真有效操作日志，不产生产品观测计数器。
  when(issue || linearIssue) { printf(p"WORK tag=${pick(tags)} kind=${ph}\n") }
}

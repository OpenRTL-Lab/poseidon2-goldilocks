/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/src/main/scala/zk/poseidon2/Environment.scala
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
import chisel3.util.experimental.loadMemoryFromFileInline

/** C++不触碰原生协议。性能、背压、错误负控均由此ENV执行。 */
class Poseidon2SimTop(cfg: PoseidonConfig, directory: String, tasks: Int) extends Module {
  val io = IO(new Bundle {
    val trigger = Input(Bool())
    val mode = Input(UInt(3.W)) // 0饱和，1协议，2复位，3负控，4单任务，5禁启动缓冲
    val done = Output(Bool())
    val pass = Output(Bool())
    val mismatches = Output(UInt(32.W))
    val checked = Output(UInt(32.W))
    val cycles = Output(UInt(32.W))
  })
  val running = RegInit(false.B)
  val cycle = RegInit(0.U(32.W))
  val issued = RegInit(0.U(32.W))
  val completed = RegInit(0.U(32.W))
  val errors = RegInit(0.U(32.W))
  val checked = RegInit(0.U(32.W))
  val accepted = Reg(Vec(tasks, UInt(32.W)))
  val seen = RegInit(VecInit(Seq.fill(tasks)(false.B)))
  val taskBits = log2Ceil(tasks)
  val resetDone = RegInit(false.B)
  val abort = running && io.mode === 2.U && cycle === 1000.U && !resetDone
  val dut = withReset(reset.asBool || abort) { Module(new GoldilocksPoseidon2(cfg)) }
  val input = Mem(tasks*12, UInt(64.W))
  val expected = Mem(tasks*12, UInt(64.W))
  loadMemoryFromFileInline(input, directory+"/input.hex")
  loadMemoryFromFileInline(expected, directory+"/expected.hex")
  dut.io.enable := !(io.mode === 1.U && cycle(5,0) < 3.U) && !(io.mode === 5.U && cycle < 64.U)
  val heldRequest = RegInit(false.B)
  dut.io.request.valid := running && issued < tasks.U && !abort &&
    (heldRequest || !(io.mode === 1.U && cycle(3,0) === 0.U)) &&
    !(io.mode === 4.U && issued =/= completed)
  dut.io.request.bits.tag := issued
  for(i <- 0 until 12) dut.io.request.bits.words(i) := input(issued*12.U+i.U)
  dut.io.response.ready := !(io.mode === 1.U && cycle(5,0) < 17.U)
  when(io.trigger) { running := true.B }
  when(running) { cycle := cycle + 1.U }
  when(abort) {
    heldRequest := false.B
    resetDone := true.B;issued := 0.U;completed := 0.U;errors := 0.U;checked := 0.U
    seen.foreach(_ := false.B)
  }.otherwise {
    heldRequest := dut.io.request.valid && !dut.io.request.ready
    when(io.mode===5.U && cycle===63.U) { assert(issued===2.U && completed===0.U) }
    when(dut.io.request.fire) {
      accepted(issued(taskBits-1,0)) := cycle
      printf(p"ACCEPT tag=${issued} cycle=${cycle}\n")
      issued := issued + 1.U
    }
    when(dut.io.response.fire) {
      val tag = dut.io.response.bits.tag
      assert(tag < tasks.U && !seen(tag(taskBits-1,0)))
      seen(tag(taskBits-1,0)) := true.B
      val bad = (0 until 12).map(i => dut.io.response.bits.words(i) =/=
        (expected(tag*12.U+i.U) ^ Mux(io.mode === 3.U && tag === 0.U && i.U === 0.U, 1.U, 0.U)))
      errors := errors + PopCount(bad)
      checked := checked + 12.U
      completed := completed + 1.U
      printf(p"COMPLETE tag=${tag} cycle=${cycle} latency=${cycle-accepted(tag(taskBits-1,0))} errors=${PopCount(bad)}\n")
      when(completed === (tasks-1).U) { running := false.B }
    }
  }
  // functional ready/valid稳定性；非性能带宽扫描。
  val held = RegNext(dut.io.response.valid && !dut.io.response.ready, false.B)
  val previous = RegNext(dut.io.response.bits.asUInt)
  when(held && !abort) {
    assert(dut.io.response.valid && dut.io.response.bits.asUInt === previous)
  }
  val previousRequest = RegNext(dut.io.request.bits.asUInt)
  when(heldRequest && !abort) {
    assert(dut.io.request.valid && dut.io.request.bits.asUInt===previousRequest)
  }
  io.done := completed === tasks.U || cycle > 10000000.U
  io.pass := completed === tasks.U && errors === 0.U
  io.mismatches := errors;io.checked := checked;io.cycles := cycle
}

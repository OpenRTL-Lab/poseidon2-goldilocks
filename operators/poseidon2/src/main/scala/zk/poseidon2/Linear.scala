/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/src/main/scala/zk/poseidon2/Linear.scala
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

/** 独立共享线性引擎，所有结果9周期返回，II=1。 */
class PoseidonLinear extends Module {
  val io = IO(new Bundle {
    val words = Input(Vec(12, UInt(64.W))); val total = Input(UInt(64.W))
    val phase = Input(UInt(4.W)); val result = Output(Vec(12, UInt(64.W)))
  })
  import PipeField._
  val x = io.words.toSeq
  val extA = x.grouped(4).flatMap { q =>
    val a = add(q(0),q(1)); val b = add(q(2),q(3)) // t2
    val s = add(a,b) // t4
    val u = add(s,delay(q(1),4)); val v = add(s,delay(q(3),4)) // t6
    val twice0 = add(q(0),q(0)); val twice2 = add(q(2),q(2))
    Seq(add(u,delay(a,4)),add(u,delay(twice2,4)),
      add(v,delay(b,4)),add(v,delay(twice0,4))) // t8
  }.toSeq
  val column = (0 until 4).map(k => add(add(x(k),x(4+k)),delay(x(8+k),2))) // t4
  val extB = (0 until 12).map(i => delay(add(delay(x(i),4),column(i%4)),2)) // t8
  def tree(q: Seq[UInt]): UInt = if(q.size == 1) q.head else
    tree(q.grouped(2).map(t => if(t.size == 2) add(t(0),t(1)) else delay(t.head,2)).toSeq)
  val sum = tree(x) // t8
  val diagonal = Seq(
    delay(neg(add(x(0),x(0))),2),delay(x(1),5),delay(add(x(2),x(2)),3),delay(half(x(3)),4),
    delay(add(add(x(4),x(4)),delay(x(4),2)),1),delay(add(add(x(5),x(5)),add(x(5),x(5))),1),
    delay(neg(half(x(6))),3),neg(add(add(x(7),x(7)),delay(x(7),2))),
    neg(add(add(x(8),x(8)),add(x(8),x(8)))),
    delay(half(half(x(9))),3),delay(neg(half(half(x(10)))),2),delay(half(half(half(x(11)))),2)) // t5
  val intB = diagonal.map(v => delay(add(v,delay(io.total,5)),1)) // t8
  val phase8 = ShiftRegister(io.phase,8)
  for(i <- 0 until 12) {
    io.result(i) := RegNext(MuxLookup(phase8,0.U(64.W))(Seq(
      1.U -> extA(i),2.U -> extB(i),7.U -> sum,8.U -> intB(i))))
  }
}

/** 输入选择1级、模加2级、模乘11级，固定14周期。 */
class PoseidonSbox extends Module {
  val io = IO(new Bundle {
    val words = Input(Vec(12,UInt(64.W))); val word = Input(UInt(4.W)); val round = Input(UInt(5.W))
    val phase = Input(UInt(4.W)); val base = Input(UInt(64.W))
    val x2 = Input(UInt(64.W)); val x3 = Input(UInt(64.W)); val x4 = Input(UInt(64.W))
    val product = Output(UInt(64.W)); val normalized = Output(UInt(64.W))
  })
  val rc = VecInit(Parameters.RC.map(row => VecInit(row.map(_.U(64.W)))))
  val selected = RegNext(io.words(io.word)); val constant = RegNext(rc(io.round)(io.word))
  val a = RegNext(MuxLookup(io.phase,0.U(64.W))(Seq(4.U->io.x2,5.U->io.x2,6.U->io.x3)))
  val b = RegNext(MuxLookup(io.phase,0.U(64.W))(Seq(4.U->io.base,5.U->io.x2,6.U->io.x4)))
  val z = PipeField.add(selected,constant)
  val square = ShiftRegister(io.phase===3.U,3,false.B,true.B)
  val mul = Module(new PoseidonGoldilocksMul)
  mul.io.a := Mux(square,z,ShiftRegister(a,2))
  mul.io.b := Mux(square,z,ShiftRegister(b,2))
  io.product := mul.io.product
  io.normalized := ShiftRegister(z,11)
}

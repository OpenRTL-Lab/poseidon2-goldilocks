/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/src/main/scala/zk/poseidon2/Arithmetic.scala
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

/** 显式并行前缀进位；无工艺或FPGA原语依赖。 */
object Prefix {
  def add(a: UInt, b: UInt, width: Int, carryIn: Bool = false.B): UInt = {
    val left = Wire(UInt(width.W)); left := a
    val right = Wire(UInt(width.W)); right := b
    val bits = left ^ right
    var prop = bits; var gen = left & right
    var distance = 1
    while(distance < width) {
      val shiftedGen = (gen << distance)(width-1, 0)
      val shiftedProp = Cat(prop(width-distance-1, 0), ((BigInt(1)<<distance)-1).U(distance.W))
      val nextGen = gen | (prop & shiftedGen)
      val nextProp = prop & shiftedProp
      gen = nextGen; prop = nextProp; distance *= 2
    }
    val carries = ((gen << 1) | ((prop << 1) & Fill(width+1, carryIn)) | carryIn.asUInt)(width-1, 0)
    bits ^ carries
  }
  def sub(a: UInt, b: UInt, width: Int): UInt = {
    val right = Wire(UInt(width.W)); right := b
    add(a, ~right, width, true.B)
  }
}

/** 两级规范模加：每级最多一次宽进位计算。 */
class PoseidonModAdd extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(64.W)); val b = Input(UInt(64.W)); val sum = Output(UInt(64.W))
  })
  val raw = RegNext(Prefix.add(Cat(0.U(1.W), io.a), Cat(0.U(1.W), io.b), 65))
  val complement = ((BigInt(1)<<65)-1-Parameters.P).U(65.W)
  val reduced = Prefix.add(Cat(0.U(1.W), raw), Cat(0.U(1.W), complement), 66, true.B)
  io.sum := RegNext(Mux(reduced(65), reduced(63,0), raw(63,0)))
}

object PipeField {
  def delay(a: UInt, cycles: Int): UInt = if(cycles == 0) a else ShiftRegister(a, cycles)
  def add(a: UInt, b: UInt): UInt = {
    val m = Module(new PoseidonModAdd); m.io.a := a; m.io.b := b; m.io.sum
  }
  def half(a: UInt): UInt = RegNext(Prefix.add(Cat(0.U(1.W),a),
    Mux(a(0), Parameters.P.U(65.W), 0.U(65.W)), 65)(64,1))
  def neg(a: UInt): UInt = RegNext(Mux(a === 0.U, 0.U(64.W), Prefix.sub(Parameters.P.U(64.W),a,64)))
}

object PoseidonGoldilocksMul {
  val Latency = 11
}

/** Standard-cell-oriented arithmetic. Four 32x32 products, not FPGA DSP limbs.
  * Fixed eleven-cycle latency and II=1; physical frequency belongs to DC owner.
  */
class PoseidonGoldilocksMul extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val product = Output(UInt(64.W))
  })
  private val p = BigInt("ffffffff00000001", 16).U(66.W)
  private val p0 = RegNext(io.a(31, 0) * io.b(31, 0))
  private val p1 = RegNext(io.a(31, 0) * io.b(63, 32))
  private val p2 = RegNext(io.a(63, 32) * io.b(31, 0))
  private val p3 = RegNext(io.a(63, 32) * io.b(63, 32))
  private val outerLow = RegNext(p0)
  private val cross = RegNext(Prefix.add(p1,p2,65))
  private val outerHigh = RegNext(p3)
  private val low = RegNext(Prefix.add(outerLow,Cat(cross(31, 0), 0.U(32.W)),65))
  private val crossHigh = RegNext(cross(64, 32))
  private val highDelayed = RegNext(outerHigh)
  private val productLow = RegNext(low(63, 0))
  private val productHigh = RegNext(Prefix.add(Cat(0.U(1.W), highDelayed),
    Cat(0.U(32.W), crossHigh),65,low(64)))
  private val t0 = RegNext(productLow(31, 0))
  private val highSum = RegNext(Prefix.add(productLow(63, 32),productHigh(31, 0),33))
  private val subtrahend = RegNext(Prefix.add(productHigh(31, 0),productHigh(63, 32),33))
  private val positive = RegNext(Prefix.add(Cat(0.U(1.W), highSum, t0),p,66))
  private val delayedSub = RegNext(subtrahend)
  private val candidate = RegNext(Prefix.sub(positive,Cat(0.U(33.W), delayedSub),66))
  private val candidateDelay = RegNext(candidate)
  private val firstSubtract = RegNext(candidate >= p)
  private val once = RegNext(Mux(firstSubtract, Prefix.sub(candidateDelay,p,66), candidateDelay))
  private val onceDelay = RegNext(once)
  private val secondSubtract = RegNext(once >= p)
  private val twice = RegNext(Mux(secondSubtract, Prefix.sub(onceDelay,p,66), onceDelay))
  io.product := twice(63, 0)
}

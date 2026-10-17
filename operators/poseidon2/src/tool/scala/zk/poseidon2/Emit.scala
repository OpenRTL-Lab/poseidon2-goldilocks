/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/src/tool/scala/zk/poseidon2/Emit.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.poseidon2
import circt.stage.ChiselStage
import java.nio.file.{Files, Paths}

/** 生成产品及配套验证环境；所有运行文件使用项目内的相对路径。 */
object Emit extends App {
  require(args.length == 4, "需要输出目录、上下文数、向量目录、任务数")
  val path = Paths.get(args(0)).normalize()
  val vectors = Paths.get(args(2)).normalize()
  val tasks = args(3).toInt
  require(!path.isAbsolute && path.startsWith(Paths.get("build")) && !Files.exists(path))
  require(!vectors.isAbsolute && vectors.startsWith(Paths.get("build")))
  require(tasks >= 4 * args(1).toInt && tasks <= 4096 && (tasks & (tasks - 1)) == 0)
  Files.createDirectories(path)
  ChiselStage.emitSystemVerilog(
    new Poseidon2SimTop(PoseidonConfig(args(1).toInt), vectors.toString, tasks),
    firtoolOpts = Array("-O=release", "-disable-all-randomization",
      "--lowering-options=disallowLocalVariables", "-split-verilog", s"-o=$path"))
}

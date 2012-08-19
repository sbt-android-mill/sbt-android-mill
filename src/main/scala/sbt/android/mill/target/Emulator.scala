/**
 * sbt-android-mill - simple-build-tool multi-thread plugin with profiling
 *
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.android.mill.target

import sbinary.DefaultProtocol.StringFormat
import sbt._
import sbt.Cache._
import sbt.Keys._
import sbt.android.mill.MillKeys._

object Emulator extends Target with Test {
  lazy val stagePrepareKey = emulatorStagePrepare
  lazy val stageCoreKey = emulatorStageCore
  lazy val stageFinalizerKey = emulatorStageFinalizer
  override val tag = "emulator"

  val settings: Seq[Project.Setting[_]] = targetSettings ++ Seq(
    emulatorStagePrepare <<= targetStagePrepareTask(true),
    emulatorStagePrepare <<= emulatorStagePrepare dependsOn (preStagePrepare),
    emulatorStageCore <<= targetStageCoreTask(true, emulatorStageCore),
    emulatorStageCore <<= emulatorStageCore dependsOn (emulatorStagePrepare, targetStageReinstall(true)),
    emulatorStageFinalizer <<= stageFinalizerTask,
    emulatorStageFinalizer <<= emulatorStageFinalizer dependsOn (emulatorStagePrepare, emulatorStageCore),
    emulatorStageUninstall <<= targetStageUninstallTask(true, false, emulatorStageUninstall.key.label),
    emulatorStageUninstall <<= emulatorStageUninstall dependsOn emulatorStagePrepare,
    emulatorStageUninstallSoft <<= targetStageUninstallTask(true, true, emulatorStageUninstallSoft.key.label),
    emulatorStageUninstallSoft <<= emulatorStageUninstallSoft dependsOn emulatorStagePrepare,
    emulatorTest <<= instrumentationTestAction(true, emulatorTest),
    emulatorTest <<= emulatorTest dependsOn emulatorStagePrepare,
    emulatorTestOnly <<= InputTask(loadForParser(definedTestNames in sbt.Test)((s, i) =>
      testParser(s, i getOrElse Nil)))(test => runTestOnly(true, emulatorTestOnly)(test)),
    emulatorTestOnly <<= emulatorTestOnly dependsOn emulatorStagePrepare)
}

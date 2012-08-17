/**
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

package sbt.android.mill.device

import sbt._
import sbt.android.mill.MillKeys._
import sbt.Keys._
object Device extends Target {
  lazy val stagePrepareKey = deviceStagePrepare
  lazy val stageCoreKey = deviceStageCore
  lazy val stageFinalizerKey = deviceStageFinalizer

  lazy val settings: Seq[Project.Setting[_]] = Seq(
    adbTimeout := DefaultADBTimeout,
    reinstallKeepData := DefaultReinstallKeepData,
    deviceStagePrepare <<= targetStagePrepareTask(false),
    deviceStagePrepare <<= deviceStagePrepare dependsOn (preStagePrepare),
    deviceStageCore <<= targetStageCoreTask(false),
    deviceStageCore <<= deviceStageCore dependsOn (deviceStagePrepare, targetStageReinstall(false)),
    deviceStageFinalizer <<= stageFinalizerTask,
    deviceStageFinalizer <<= deviceStageFinalizer dependsOn (deviceStagePrepare, deviceStageCore),
    deviceStageUninstall <<= targetStageUninstallTask(false, false, deviceStageUninstall.key.label),
    deviceStageUninstall <<= deviceStageUninstall dependsOn deviceStagePrepare,
    deviceStageUninstallSoft <<= targetStageUninstallTask(false, true, deviceStageUninstallSoft.key.label),
    deviceStageUninstallSoft <<= deviceStageUninstallSoft dependsOn deviceStagePrepare)
}

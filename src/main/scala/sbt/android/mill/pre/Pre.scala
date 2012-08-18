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

package sbt.android.mill.pre

import sbt.Keys._
import sbt._
import sbt.android.mill.MillKeys._
import sbt.android.mill.MillStage
import sbt.android.mill.Mill

object Pre extends MillStage {
  lazy val stagePrepareKey = preStagePrepare
  lazy val stageCoreKey = preStageCore
  lazy val stageFinalizerKey = preStageFinalizer
  val settings: Seq[Project.Setting[_]] = Seq(
    preStagePrepare <<= preStagePrepareTask,
    preStageCore <<= preStageCoreTask,
    preStageCore <<= preStageCore dependsOn preStagePrepare,
    preStageFinalizer <<= stageFinalizerTask,
    preStageFinalizer <<= preStageFinalizer dependsOn (preStagePrepare, preStageCore))

  def preStagePrepareTask = (state, streams) map ((state, streams) =>
    stagePrepare(state, streams.log, true))
  def preStageCoreTask = (streams) map ((s) => task(s.log) {})
}

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

package sbt.android.mill.compile

import sbt._
import sbt.Keys._
import sbt.android.mill.MillKeys._
import sbt.android.mill.MillStage

object Compile extends MillStage {
  lazy val stagePrepareKey = compileStagePrepare
  lazy val stageCoreKey = compileStageCore
  lazy val stageFinalizerKey = compileStageFinalizer
  lazy val settings: Seq[Project.Setting[_]] = Seq(
    compileStagePrepare <<= stagePrepareTask,
    compileStageCorePre <<= compileStageCorePreTask,
    compileStageCore <<= compileStageCoreTask,
    compileStageCore <<= compileStageCore dependsOn (compileStagePrepare, compile in Configurations.Compile),
    compileStageFinalizer <<= stageFinalizerTask,
    compileStageFinalizer <<= compileStageFinalizer dependsOn compileStageCore,
    compile in Configurations.Compile <<= compile in Configurations.Compile dependsOn compileStageCorePre)

  def compileStageCorePreTask = (streams) map ((s) => stageCorePre(s.log))
  def compileStageCoreTask = (streams) map ((s) => stageCorePost())
}

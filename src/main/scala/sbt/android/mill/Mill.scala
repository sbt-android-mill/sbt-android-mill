/**
 * sbt-android-mill android plugin with profiling and multi-thread support
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

package sbt.android.mill

import sbt._
import Keys._
import sbt.Plugin
import sbt.android.mill.MillKeys._
import stopwatch.StopwatchGroup
import java.lang.System

/**
 * main plugin class
 * task sequence related to http://developer.android.com/tools/building/index.html
 * before - pre
 * stage 1 - aidl
 * stage 2 - aapt
 * stage 3 - compile
 * stage 4 - dex
 * stage 5 - apkBuilder
 * stage 6 - jarSigner
 * stage 7 - zipAlign
 * stage 8 - publish
 */
abstract class Mill extends Plugin {
  val millSettings: Seq[Project.Setting[_]]
  def millConf = config("android-mill") extend (Compile)

  lazy val compositeSettings: Seq[Project.Setting[_]] = MillSettings.defaultSettings ++
    MillSettings.runtimeSettings ++
    MillSettings.delegateSettings ++
    MillSettings.derivativeSettings ++
    Mill.settings ++
    Mill.tasksSequence

}

object Mill {
  @volatile var buildStartTime = System.currentTimeMillis()
  @volatile var profilingGroups = Seq[StopwatchGroup]()
  /*    stage1_prepare <<= stage1_prepareTask,
    stage2_AAPT <<= stage2_AAPTTask,
    stage2_AAPT <<= stage2_AAPT dependsOn (stage1_prepare),
    stage3_aidl <<= stage3_aidlTask,
    stage3_aidl <<= stage3_aidl dependsOn (stage1_prepare),
    stage4_compile <<= stage4_compileTask,
    stage4_compile <<= stage4_compile dependsOn (stage2_AAPT, stage3_aidl)))*/
  val tasksSequence = Seq(
    aidlStageCore <<= aidlStageCore dependsOn (preStageCore))

  val settings = Seq(
    statistics <<= statisticsTask)

  def statisticsTask = (streams) map (s => dumpStopWatchStatistics(s.log))

  def getStopwatchGroup(name: String) =
    Mill.profilingGroups.find(_.name == name) match {
      case Some(group) =>
        group
      case None =>
        val group = new StopwatchGroup(name)
        group.enabled = true
        group.enableOnDemand = true
        Mill.profilingGroups = Mill.profilingGroups :+ group
        group
    }
  def dumpStopWatchStatistics(implicit log: Logger) = profilingGroups.sortBy(_.name).foreach {
    group =>
      log.info("""process "%s" stopwatch statistics:""".format(group.name))
      group.names.toSeq.sorted.foreach(name =>
        log.info(group.snapshot(name).toShortString))
  }
}

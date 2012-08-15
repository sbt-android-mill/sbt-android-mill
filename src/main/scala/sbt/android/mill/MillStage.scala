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
import stopwatch.Stopwatch
import sbt.Logger
import java.util.concurrent.TimeUnit

trait MillStage {
  /** stage stopwatch group */
  @volatile protected var profiling: Option[stopwatch.Stopwatch] = None
  /** task that prepare something for stageCoreKey, for example start emulator */
  val stagePrepareKey: TaskKey[Unit]
  /** task that do main job, as usual it depends on stagePrepareKey, and previous stage stageCoreKey */
  val stageCoreKey: TaskKey[_]
  /** task that user launch from sbt or user defined tasks, for example it will close emulator after testing */
  val stageFinalizerKey: TaskKey[Unit]
  /** stage settings */
  val settings: Seq[Project.Setting[_]]
  /** tag for logger */
  lazy val tag = stageFinalizerKey.key.label

  def stopwatchGroup = Mill.getStopwatchGroup(tag)
  def stagePrepareTask = (streams) map {
    (s) =>
      val (minutes, seconds) = getRunTime
      s.log.debug(header + " preparing")
  }
  def stageCorePre(log: sbt.Logger) {
    profiling = Some(stopwatchGroup.start(tag))
    val (minutes, seconds) = getRunTime
    log.info(header + " start core thread id " + Thread.currentThread.getId)
  }
  def stageCorePost() {
    profiling.foreach(_.stop)
  }
  def stageFinalizerTask = (streams) map {
    (s) =>
      s.log.debug(header + " finalizing sequence")
  }
  protected def header = {
    val (minutes, seconds) = getRunTime
    "%dm%ds >>> [%s]".format(minutes, seconds, tag)
  }
  protected def getRunTime(): (Long, Long) = {
    val millis = System.currentTimeMillis() - Mill.buildStartTime
    (TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
  }
}

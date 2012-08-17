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

package sbt.android.mill

import java.util.concurrent.TimeUnit

import scala.collection.mutable.HashMap
import scala.collection.mutable.Publisher
import scala.collection.mutable.SynchronizedMap

import sbt._
import sbt.Keys._
import stopwatch.Stopwatch

/*
 * don't use lazy val, stageFinalizerKey.key.label hung in deadlock 
 */
trait MillStage {
  /** stage tasks stopwatch group */
  protected val profiling = new HashMap[String, Stopwatch]() with SynchronizedMap[String, Stopwatch]
  /**
   * task that prepare something for stageCoreKey, for example start emulator
   * may be called multiple times, must provide guards against concurrent calls from different threads
   * best suited for notification, that something must be prepared
   */
  val stagePrepareKey: TaskKey[_]
  /** task that do main job, as usual it depends on stagePrepareKey, and previous stage stageCoreKey */
  val stageCoreKey: TaskKey[_]
  /** task that user launch from sbt or user defined tasks, for example it will close emulator after testing */
  val stageFinalizerKey: TaskKey[_]
  /** stage settings */
  val settings: Seq[Project.Setting[_]]
  /** tag for logger */
  val tag = stageFinalizerKey.key.label

  def stagePrepareTask = (state, streams) map ((state, streams) => stagePrepare(state, streams.log))
  def stagePrepare(state: State, log: sbt.Logger, firstPrepareInSequence: Boolean = false) {
    Mill.synchronized { if (firstPrepareInSequence) MillStage.publish(MillStage.Event.MillStart(state)) }
    log.debug(header() + "preparing")
  }
  def taskPre(log: sbt.Logger, tag: String = tag) {
    profiling(tag) = stopwatchGroup.start(tag)
    log.info(header(tag) + "start task")
  }
  def taskPost(tag: String = tag) {
    profiling.get(tag).foreach(_.stop)
  }
  def task[F](log: sbt.Logger, tag: String = tag)(f: => F): F = {
    taskPre(log: sbt.Logger, tag)
    val result = f
    taskPost(tag)
    result
  }
  def stageFinalizerTask = (state, streams) map ((state, streams) => stageFinalizer(state, streams.log))
  def stageFinalizer(state: State, log: sbt.Logger) {
    log.debug(header() + "finalizing sequence")
    Mill.synchronized { MillStage.publish(MillStage.Event.MillStop(state)) }
  }
  def stopwatchGroup = Mill.getStopwatchGroup(tag)
  protected def header(tag: String = tag) = {
    val (minutes, seconds) = getRunTime
    "%dm%ds >>> [%s:%d] ".format(minutes, seconds, tag, Thread.currentThread.getId())
  }
  protected def getRunTime(): (Long, Long) = {
    val millis = System.currentTimeMillis() - Mill.buildStartTime
    (TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
  }
}

sealed trait MillEvent

object MillStage extends Publisher[MillEvent] {
  override protected def publish(event: MillEvent) =
    super.publish(event)
  object Event {
    case class MillStart(state: State) extends MillEvent
    case class MillStop(state: State) extends MillEvent
  }
}

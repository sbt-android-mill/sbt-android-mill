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

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import scala.collection.mutable.Subscriber

import sbt._
import sbt.Keys._
import sbt.android.mill.MillEvent
import sbt.android.mill.MillKeys._
import sbt.android.mill.MillStage
import sbt.android.mill.util.ReduceLogLevelWrapper
import sbt.android.mill.util.Util

trait Target extends MillStage {
  val tag: String
  protected val connected = new AtomicReference[Option[Boolean]](None)
  val DefaultADBName = "adb"
  val DefaultADBTimeout = 30000 // 30s
  val DefaultADBLogLevel = Level.Debug
  val DefaultReinstallKeepData = true
  val subscriber = new Subscriber[MillEvent, MillStage.type#Pub] {
    def notify(pub: MillStage.type#Pub, event: MillEvent) = event match {
      case MillStage.Event.MillStart(state) =>
        connected.set(None)
      case MillStage.Event.MillStop(state) =>
    }
  }
  MillStage.subscribe(subscriber)

  protected val targetSettings: Seq[Project.Setting[_]] = Seq(
    targetADBName := DefaultADBName,
    targetADBPath <<= (platformToolsPath, targetADBName)(_ / _),
    targetADBTimeout := DefaultADBTimeout,
    targetADBLogLevel := DefaultADBLogLevel,
    targetReinstallKeep := DefaultReinstallKeepData)

  def targetStagePrepareTask(emulator: Boolean) =
    (targetADBPath, targetADBTimeout, targetADBLogLevel, state, streams) map {
      (targetADBPath, targetADBTimeout, targetADBLogLevel, state, streams) =>
        super.stagePrepare(state, streams.log)
        Util.thread { connectToTarget(emulator, targetADBPath, targetADBTimeout, targetADBLogLevel, streams.log) }; ()
    }
  def targetStageCoreTask(emulator: Boolean, taskTag: TaskKey[_]): Project.Initialize[Task[Unit]] =
    (packageApkPath, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) map {
      (packageApkPath, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) =>
        val taskTagLabel = taskTag.key.label
        task(streams.log, taskTagLabel) {
          streams.log.info(header(taskTagLabel) + "install " + packageApkPath.getAbsolutePath() + " to " + tag)
          if (connection(targetADBTimeout))
            Target.command(emulator, targetADBPath.getAbsolutePath(), () => header(taskTagLabel), targetADBLogLevel, streams.log,
              "install", "-r ", packageApkPath.getAbsolutePath())
          else
            streams.log.error(header(taskTagLabel) + "unable to install " + packageApkPath.getAbsolutePath() + ", " + tag + " not found")
        }
    }
  def targetStageReinstall(emulator: Boolean) = (targetADBPath, manifestPackage, targetReinstallKeep,
    targetADBTimeout, targetADBLogLevel, streams) map {
      (targetADBPath, manifestPackage, targetReinstallKeep, targetADBTimeout, targetADBLogLevel, streams) =>
        val tag = (emulator, targetReinstallKeep) match {
          case (true, true) => emulatorStageUninstallSoft.key.label
          case (true, false) => emulatorStageUninstall.key.label
          case (false, true) => deviceStageUninstallSoft.key.label
          case (false, false) => deviceStageUninstall.key.label
        }
        stopwatchGroup(tag) {
          uninstall(emulator, targetReinstallKeep, tag, manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams)
        }
    }
  def targetStageUninstallTask(emulator: Boolean, keepDataCache: Boolean, key: String) =
    (manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) map {
      (manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) =>
        stopwatchGroup(deviceStageUninstall.key.label) {
          uninstall(emulator, keepDataCache, key, manifestPackage,
            targetADBPath, targetADBTimeout, targetADBLogLevel, streams)
        }
    }
  /*
   * helpers
   */
  def connection(timeoutMillis: Int): Boolean = {
    val start = System.currentTimeMillis
    var elapsed = 0L
    connected.synchronized {
      while (connected.get() == None && elapsed < timeoutMillis) {
        connected.wait(timeoutMillis - elapsed)
        elapsed = System.currentTimeMillis - start
      }
    }
    connected.get() getOrElse false
  }
  def connectToTarget(emulator: Boolean, targetADBPath: File, targetADBTimeout: Int, targetADBLogLevel: Level.Value, log: Logger): Boolean = synchronized {
    connected.get match {
      case Some(true) =>
        if (checkConnection(emulator, targetADBPath, targetADBLogLevel, log)) {
          connected.set(Some(true))
          connected.synchronized { connected.notifyAll() }
          return true
        }
        connected.set(None)
        connectToTarget(emulator, targetADBPath, targetADBTimeout, targetADBLogLevel, log)
      case None =>
        val adbThread = Util.thread {
          log.info(header() + "adb not connected, tring to connect to " + tag)
          Target.command(emulator, targetADBPath.getAbsolutePath(), () => header(), targetADBLogLevel, log, "wait-for-device") match {
            case (true, output) =>
              log.info(header() + "connected to " + tag)
              connected.set(Some(true))
              connected.synchronized { connected.notifyAll() }
            case (false, output) =>
              log.error(header() + "unable to connect to " + tag)
              connected.set(Some(false))
              connected.synchronized { connected.notifyAll() }
          }
        }
        connected.get match {
          case Some(true) => true
          case Some(false) =>
            adbThread.interrupt()
            false
          case None =>
            if (connection(targetADBTimeout)) {
              true
            } else {
              adbThread.interrupt()
              false
            }
        }
    }
  }
  def checkConnection(emulator: Boolean, targetADBPath: File, targetADBLogLevel: Level.Value, log: Logger): Boolean =
    Target.command(emulator, targetADBPath.getAbsolutePath(), () => header(), targetADBLogLevel, log, "get-state") match {
      case (true, logs) if logs.exists(s => s.contains("device") || s.contains("emulator")) => true
      case _ => false
    }
  def uninstall(emulator: Boolean, keepDataCache: Boolean, key: String, manifestPackage: String,
    targetADBPath: File, targetADBTimeout: Int, targetADBLogLevel: Level.Value, streams: TaskStreams) {
    val target = if (emulator) "emulator" else "device"
    streams.log.info(header(key) + "uninstall " + manifestPackage + " from " + target)
    if (connection(targetADBTimeout)) {
      val args = if (keepDataCache) Seq("shell", "pm", "uninstall", "-k", manifestPackage) else Seq("uninstall", manifestPackage)
      Target.command(emulator, targetADBPath.getAbsolutePath(), () => header(key), targetADBLogLevel, streams.log, args: _*)
    } else
      streams.log.error(header(key) + "unable to uninstall " + manifestPackage + ", " + target + " not found")
  }
}

object Target {
  /**
   * true on success
   */
  def command(emulator: Boolean, targetADBPath: String, maxLevel: Level.Value, log: Logger, action: String*): (Boolean, Seq[String]) =
    command(emulator, targetADBPath, () => "", maxLevel, log, action: _*)
  def command(emulator: Boolean, targetADBPath: String, prefix: () => String, maxLevel: Level.Value, log: Logger, action: String*): (Boolean, Seq[String]) = {
    val logger = new ReduceLogLevelWrapper(log, maxLevel, () => prefix() + "adb output: ")
    val args = Seq(targetADBPath, if (emulator) "-e" else "-d") ++ action
    log.debug(prefix() + args.mkString(" "))
    val code = Process(args) ! logger
    // adb doesn't bother returning a non-zero exit code on failure
    if (code != 0 || logger.bufferExists(tuple => tuple._2.contains("Failure")))
      (false, logger.flush(1))
    else
      (true, logger.flush(0))
  }
}

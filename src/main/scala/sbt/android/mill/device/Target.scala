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

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.Subscriber

import sbt._
import sbt.Keys._
import sbt.android.mill.MillEvent
import sbt.android.mill.MillKeys._
import sbt.android.mill.MillStage
import sbt.android.mill.util.ReduceLogLevelWrapper
import sbt.android.mill.util.Util

trait Target extends MillStage {
  @volatile protected var adb: Option[ADB] = None
  protected val unableToConnect = new AtomicBoolean(false)
  val DefaultADBTimeout = 30000 // 30s
  val DefaultReinstallKeepData = true
  val subscriber = new Subscriber[MillEvent, MillStage.type#Pub] {
    def notify(pub: MillStage.type#Pub, event: MillEvent) = event match {
      case MillStage.Event.MillStart(state) =>
        unableToConnect.set(false)
      case MillStage.Event.MillStop(state) =>
    }
  }
  MillStage.subscribe(subscriber)

  def targetStagePrepareTask(emulator: Boolean) =
    (adbPath, adbTimeout, state, streams) map {
      (adbPath, timeout, state, streams) =>
        super.stagePrepare(state, streams.log)
        Util.thread { connectToTarget(adbPath.getAbsolutePath(), emulator, streams.log, timeout) }; ()
    }
  def targetStageCoreTask(emulator: Boolean): Project.Initialize[Task[Unit]] =
    (adbPath, packageApkPath, adbTimeout, streams) map { (adbPath, apk, timeout, s) =>
      task(s.log) {
        val target = if (emulator) "emulator" else "device"
        s.log.info(header() + "install " + apk.getAbsolutePath() + " to " + target)
        adb(timeout) match {
          case Some(adb) =>
            Target.command(adbPath.getAbsolutePath(), emulator, s.log, () => header(), "install", "-r ", apk.getAbsolutePath())
          case None =>
            s.log.error(header() + "unable to install " + apk.getAbsolutePath() + ", " + target + " not found")
        }
      }
    }
  def targetStageReinstall(emulator: Boolean) = (adbPath, manifestPackage, adbTimeout, reinstallKeepData, streams) map {
    (adbPath, manifestPackage, timeout, reinstallKeepData, streams) =>
      val tag = (emulator, reinstallKeepData) match {
        case (true, true) => emulatorStageUninstallSoft.key.label
        case (true, false) => emulatorStageUninstall.key.label
        case (false, true) => deviceStageUninstallSoft.key.label
        case (false, false) => deviceStageUninstall.key.label
      }
      stopwatchGroup(tag) {
        uninstall(emulator, reinstallKeepData, tag, adbPath, manifestPackage, timeout, streams)
      }
  }
  def targetStageUninstallTask(emulator: Boolean, keepDataCache: Boolean, key: String) =
    (adbPath, manifestPackage, adbTimeout, streams) map { (adbPath, manifestPackage, timeout, streams) =>
      stopwatchGroup(deviceStageUninstall.key.label) {
        uninstall(emulator, keepDataCache, key, adbPath, manifestPackage, timeout, streams)
      }
    }
  /*
   * helpers
   */
  def adb(timeoutMillis: Int): Option[ADB] = {
    val start = System.currentTimeMillis
    var elapsed = 0L
    unableToConnect.synchronized {
      while (!unableToConnect.get() && adb == None && elapsed < timeoutMillis) {
        unableToConnect.wait(timeoutMillis - elapsed)
        elapsed = System.currentTimeMillis - start
      }
    }
    adb
  }
  def connectToTarget(adbPath: String, emulator: Boolean, log: Logger, timeoutMillis: Int): Boolean = synchronized {
    adb match {
      case Some(adb) =>
        if (checkConnection(adbPath, emulator, log))
          return true
        this.adb = None
        connectToTarget(adbPath, emulator, log, timeoutMillis)
      case None =>
        val target = if (emulator) "emulator" else "device"
        val adbThread = Util.thread {
          log.info(header() + "adb not connected, tring to connect to " + target)
          Target.command(adbPath, emulator, log, () => header(), "wait-for-device") match {
            case (true, output) =>
              log.info(header() + "connected to " + target)
              adb = Some(new ADB)
              unableToConnect.synchronized { unableToConnect.notifyAll() }
            case (false, output) =>
              log.error(header() + "unable to connect to " + target)
              unableToConnect.set(true)
              unableToConnect.synchronized { unableToConnect.notifyAll() }
          }
        }
        adb match {
          case Some(adb) => true
          case None =>
            if (adb(timeoutMillis).isEmpty) {
              adbThread.interrupt()
              false
            } else
              true
        }
    }
  }
  def checkConnection(adbPath: String, emulator: Boolean, log: Logger): Boolean =
    Target.command(adbPath, emulator, log, () => header(), "get-state") match {
      case (true, logs) if logs.exists(s => s.contains("device") || s.contains("emulator")) => true
      case _ => false
    }
  def uninstall(emulator: Boolean, keepDataCache: Boolean, key: String, adbPath: File,
    manifestPackage: String, timeout: Int, streams: TaskStreams) {
    val target = if (emulator) "emulator" else "device"
    streams.log.info(header(key) + "uninstall " + manifestPackage + " from " + target)
    adb(timeout) match {
      case Some(adb) =>
        val args = if (keepDataCache) Seq("shell", "pm", "uninstall", "-k", manifestPackage) else Seq("uninstall", manifestPackage)
        Target.command(adbPath.getAbsolutePath(), emulator, streams.log, () => header(key), args: _*)
      case None =>
        streams.log.error(header(key) + "unable to uninstall " + manifestPackage + ", " + target + " not found")
    }
  }
}

object Target {
  /**
   * true on success
   */
  def command(adbPath: String, emulator: Boolean, log: Logger, action: String*): (Boolean, Seq[String]) =
    command(adbPath, emulator, log, () => "", action: _*)
  def command(adbPath: String, emulator: Boolean, log: Logger, prefix: () => String, action: String*): (Boolean, Seq[String]) = {
    val logger = new ReduceLogLevelWrapper(log, Level.Warn, () => prefix() + "adb output: ")
    val args = Seq(adbPath, if (emulator) "-e" else "-d") ++ action
    log.debug(prefix() + args.mkString(" "))
    val code = Process(args) ! logger
    // adb doesn't bother returning a non-zero exit code on failure
    if (code != 0 || logger.bufferExists(tuple => tuple._2.contains("Failure")))
      (false, logger.flush(1))
    else
      (true, logger.flush(0))
  }
}

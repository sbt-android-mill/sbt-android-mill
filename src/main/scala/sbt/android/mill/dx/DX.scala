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

package sbt.android.mill.dx

import java.io.{ File => JFile }
import sbt._
import sbt.Keys._
import sbt.android.mill.MillStage
import sbt.android.mill.MillKeys._
import sbt.android.mill.Mill
import sbt.android.mill.util.ReduceLogLevelWrapper

object DX extends MillStage {
  lazy val stagePrepareKey = dxStagePrepare
  lazy val stageCoreKey = dxStageCore
  lazy val stageFinalizerKey = dxStageFinalizer
  val DefaultDXName = "dx"
  val DefaultDxOpts = ("-JXmx512m", None)
  val DefaultProjectDexName = "classes.dex"

  lazy val settings: Seq[Project.Setting[_]] = Seq(
    dxName := DefaultDXName,
    dxInputs <<= dxInputsTask,
    dxOpts := DefaultDxOpts,
    dxPath <<= (platformToolsPath, dxName)(_ / _),
    dxProjectDexName := DefaultProjectDexName,
    dxProjectDexPath <<= (target, dxProjectDexName)(_ / _),
    dxStagePrepare <<= stagePrepareTask,
    dxStagePrepare <<= dxStagePrepare dependsOn (preStagePrepare, makeManagedJavaPath),
    dxStageCore <<= dxStageCoreTask,
    dxStageCore <<= dxStageCore dependsOn dxStagePrepare,
    dxStageFinalizer <<= stageFinalizerTask,
    dxStageFinalizer <<= dxStageFinalizer dependsOn (dxStagePrepare, dxStageCore))

  def dxStageCoreTask: Project.Initialize[Task[File]] =
    (dxPath, dxInputs, dxOpts, proguardOptimizations, classDirectory, dxProjectDexPath, scalaInstance, streams) map {
      (dxPath, dxInputs, dxOpts, proguardOptimizations, classDirectory, classesDexPath, scalaInstance, streams) =>
        task(streams.log) {
          val logger = new ReduceLogLevelWrapper(streams.log, Level.Debug, () => header() + "dx output: ")
          def dexing(inputs: Seq[JFile], output: JFile) {
            val uptodate = output.exists && inputs.forall(input =>
              input.isDirectory match {
                case true =>
                  (input ** "*").get.forall(_.lastModified <= output.lastModified)
                case false =>
                  input.lastModified <= output.lastModified
              })

            if (!uptodate) {
              val noLocals = if (proguardOptimizations.isEmpty) "" else "--no-locals"
              val dxCmd = (Seq(dxPath.absolutePath,
                dxMemoryParameter(dxOpts._1),
                "--dex", noLocals,
                "--num-threads=" + java.lang.Runtime.getRuntime.availableProcessors,
                "--output=" + output.getAbsolutePath) ++
                inputs.map(_.absolutePath)).filter(_.length > 0)
              streams.log.debug(header() + dxCmd.mkString(" "))
              streams.log.info(header() + "Dexing " + output.getAbsolutePath)
              val code = Process(dxCmd) ! logger
              logger.flush(code)
              if (code != 0)
                sys.error(header() + "error dexing project jar")
            } else streams.log.debug(header() + "dex file " + output.getAbsolutePath + " uptodate, skipping")
          }

          // Option[Seq[String]]
          // - None standard dexing for prodaction stage
          // - Some(Seq(predex_library_regexp)) predex only changed libraries for development stage
          dxOpts._2 match {
            case None =>
              dexing(dxInputs.get, classesDexPath)
            case Some(predex) =>
              val (dexFiles, predexFiles) = predex match {
                case exceptSeq: Seq[_] if exceptSeq.nonEmpty =>
                  val (filtered, orig) = dxInputs.get.partition(file =>
                    exceptSeq.exists(filter => {
                      streams.log.debug(header() + "apply filter \"" + filter + "\" to \"" + file.getAbsolutePath + "\"")
                      file.getAbsolutePath.matches(filter)
                    }))
                  // dex only classes directory ++ filtered, predex all other
                  ((classDirectory --- scalaInstance.libraryJar).get ++ filtered, orig)
                case _ =>
                  // dex only classes directory, predex all other
                  ((classDirectory --- scalaInstance.libraryJar).get, (dxInputs --- classDirectory).get)
              }
              dexFiles.foreach(s => streams.log.debug(header() + "pack in dex \"" + s.getName + "\""))
              predexFiles.foreach(s => streams.log.debug(header() + "pack in predex \"" + s.getName + "\""))
              // dex
              dexing(dexFiles, classesDexPath)
              // predex
              predexFiles.get.foreach(f => {
                val predexPath = new JFile(classesDexPath.getParent, "predex")
                if (!predexPath.exists)
                  predexPath.mkdir
                val output = new File(predexPath, f.getName)
                val outputPermissionDescriptor = new File(predexPath, f.getName.replaceFirst(".jar$", ".xml"))
                dexing(Seq(f), output)
                val permission = <permissions><library name={ f.getName.replaceFirst(".jar$", "") } file={ "/data/" + f.getName }/></permissions>
                val p = new java.io.PrintWriter(outputPermissionDescriptor)
                try { p.println(permission) } finally { p.close() }
              })
          }
          classesDexPath
        }
    }
  def dxInputsTask = (proguardStageCore, proguardInJars, scalaInstance, classDirectory) map {
    (proguard, proguardInJars, scalaInstance, classDirectory) =>
      proguard match {
        case Some(file) => Seq(file)
        case None => (classDirectory +++ proguardInJars --- scalaInstance.libraryJar) get
      }
  }
  /*
   *  per http://code.google.com/p/android/issues/detail?id=4217, dx.bat
   *  doesn't currently support -JXmx arguments.  For now, omit them in windows.
   */
  def dxMemoryParameter(javaOpts: String) =
    if (Mill.isWindows) "" else javaOpts
}
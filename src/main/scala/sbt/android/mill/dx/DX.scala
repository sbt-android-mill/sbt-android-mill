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

/*
 * I got
 * real	0m9.516s with dx --num-threads=java.lang.Runtime.getRuntime.availableProcessors (8 cores)
 * real	0m7.445s with dx --num-threads=1
 * real	0m4.699s with dx --num-threads=1 --no-optimize
 * also --num-threads=1 is strongly recommend because fatal error in thread not affect for dx return status
 * gentoo linux 20.08.2012 Ezh
 */
object DX extends MillStage {
  lazy val stagePrepareKey = dxStagePrepare
  lazy val stageCoreKey = dxStageCore
  lazy val stageFinalizerKey = dxStageFinalizer
  val DefaultDXName = "dx"
  val DefaultDxMemoryOpt = "-JXmx512m"
  val DefaultDxOpts = Seq("--num-threads=1")
  val DefaultDxPredex = None
  val DefaultProjectDexName = "classes.dex"

  lazy val settings: Seq[Project.Setting[_]] = Seq(
    dxName := DefaultDXName,
    dxInputs <<= dxInputsTask,
    dxMemoryOpt := DefaultDxMemoryOpt,
    dxOpts := DefaultDxOpts,
    dxPredex := DefaultDxPredex,
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
    (dxPath, dxInputs, dxMemoryOpt, dxOpts, dxPredex, proguardOptimizations, classDirectory in millConf, dxProjectDexPath, scalaInstance, streams) map {
      (dxPath, dxInputs, dxMemoryOpt, dxOpts, dxPredex, proguardOptimizations, classDirectory, classesDexPath, scalaInstance, streams) =>
        task(streams.log) {
          val logger = new ReduceLogLevelWrapper(streams.log, Level.Debug, () => header() + "dx output: ")
          def dexing(inputs: Seq[JFile], output: JFile) = task(streams.log, tag + " " + output.getName()) {
            val uptodate = output.exists && inputs.forall(input =>
              input.isDirectory match {
                case true =>
                  (input ** "*").get.forall(_.lastModified <= output.lastModified)
                case false =>
                  input.lastModified <= output.lastModified
              })

            if (!uptodate) {
              val noLocals = if (proguardOptimizations.isEmpty) "" else "--no-locals"
              val dxCmd = (Seq(dxPath.absolutePath, dxMemoryParameter(dxMemoryOpt), "--dex") ++ dxOpts ++ Seq(
                noLocals,
                "--output=" + output.getAbsolutePath) ++
                inputs.map(_.absolutePath)).filter(_.length > 0)
              streams.log.info(header() + "Dexing " + output.getAbsolutePath)
              streams.log.debug(header() + dxCmd.mkString(" "))
              val code = Process(dxCmd) ! logger
              logger.flush(code)
              if (code != 0)
                sys.error(header() + "error dexing project jar")
            } else streams.log.debug(header() + "dex file " + output.getAbsolutePath + " uptodate, skipping")
          }

          // Option[Seq[String]]
          // - None standard dexing for production stage
          // - Some(Seq(predex_library_regexp)) predex only changed libraries for development stage
          dxPredex match {
            case None =>
              dexing(dxInputs.get, classesDexPath)
            case Some(predex) =>
              val (dexFiles, predexFiles) = predex match {
                case filterRegExps: Seq[_] if filterRegExps.nonEmpty =>
                  val (filtered, orig) = dxInputs.get.partition(file =>
                    filterRegExps.exists(filter => {
                      streams.log.debug(header() + "apply filter \"" + filter + "\" to \"" + file.getAbsolutePath + "\"")
                      file.getAbsolutePath.matches(filter) || file.getAbsolutePath() == classDirectory.getAbsolutePath()
                    }))
                  // dex only classes directory ++ filtered, predex all other
                  ((filtered --- scalaInstance.libraryJar).get, orig)
                case _ =>
                  val (filtered, orig) = dxInputs.get.partition(file =>
                    file.getAbsolutePath() == classDirectory.getAbsolutePath())
                  // dex only classes directory, predex all other
                  ((filtered --- scalaInstance.libraryJar).get, orig)
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
  def dxInputsTask = (proguardStageCore, proguardInJars, scalaInstance, classDirectory in millConf) map {
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
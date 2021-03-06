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

package sbt.android.mill.aapt

import sbt._
import sbt.Keys._
import sbt.android.mill.MillStage
import sbt.android.mill.MillKeys._
import sbt.android.mill.util.ReduceLogLevelWrapper

object AAPT extends MillStage {
  lazy val stagePrepareKey = aaptStagePrepare
  lazy val stageCoreKey = aaptStageCore
  lazy val stageFinalizerKey = aaptStageFinalizer
  val DefaultAaptName = "aapt"
  val DefaultAaptResourcesApkName = "resources.apk"
  val DefaultAaptVerbose = false

  val settings: Seq[Project.Setting[_]] = Seq(
    aaptApkName := DefaultAaptResourcesApkName,
    aaptApkPath <<= (target, aaptApkName)(_ / _),
    aaptName := DefaultAaptName,
    aaptPackage <<= aaptPackageTask,
    aaptPath <<= (platformToolsPath, aaptName)(_ / _),
    aaptVerbose := DefaultAaptVerbose,
    aaptStagePrepare <<= stagePrepareTask,
    aaptStagePrepare <<= aaptStagePrepare dependsOn (preStagePrepare, makeManagedJavaPath),
    aaptStageCore <<= aaptStageCoreTask,
    aaptStageCore <<= aaptStageCore dependsOn aaptStagePrepare,
    aaptStageFinalizer <<= stageFinalizerTask,
    aaptStageFinalizer <<= aaptStageFinalizer dependsOn (aaptStagePrepare, aaptStageCore),
    sourceGenerators in Compile <+= aaptStageCore)

  def aaptStageCoreTask =
    (manifestPackage, aaptPath, aaptVerbose, manifestPath, mainResPath, jarPathSDK, managedJavaPath, libraries, streams) map {
      (mPackage, aPath, verbose, mPath, resPath, jPath, javaPath, libraries, streams) =>
        task(streams.log) {
          val logger = new ReduceLogLevelWrapper(streams.log, Level.Debug, () => header() + "aapt output: ")
          if (libraries.nonEmpty)
            streams.log.debug(header() + "add libraries '" + libraries.map(_.pkgName).mkString("', '") + "'")
          val libraryResPathArgs = libraries.flatMap(_.resources.map(d => Seq("-S", d.absolutePath))).flatten
          val libraryAssetPathArgs = libraries.flatMap(_.assets.map(d => Seq("-A", d.absolutePath))).flatten
          def runAapt(`package`: String, args: String*) {
            val extraPackages = if (libraries.nonEmpty) Seq("--extra-packages", libraries.map(_.pkgName).mkString(":")) else Seq()
            val aapt = Seq(aPath.absolutePath, "package", "--auto-add-overlay", "-m", "--non-constant-id", "--custom-package", mPackage) ++
              extraPackages ++ Seq(
                "-M", mPath.head.absolutePath,
                "-S", resPath.absolutePath) ++
                libraryResPathArgs ++ Seq(
                  "-I", jPath.absolutePath,
                  "-J", javaPath.absolutePath) ++
                  libraryAssetPathArgs ++ args ++ (if (verbose) Seq("-v") else Seq())
            streams.log.debug(header() + aapt.mkString(" "))
            val code = Process(aapt) ! logger
            logger.flush(code)
            if (code != 0)
              sys.error(header() + "error generating resources")
          }
          runAapt(mPackage)
          javaPath ** "R.java" get
        }
    }
  def aaptPackageTask: Project.Initialize[Task[File]] =
    (aaptPath, aaptVerbose, manifestPath, mainResPath, mainAssetsPath, jarPathSDK, aaptApkPath, libraries, streams) map {
      (apPath, verbose, manPath, rPath, assetPath, jPath, resApkPath, libraries, streams) =>
        val taskKeyLabel = aaptPackage.key.label
        task(streams.log, taskKeyLabel) {
          val logger = new ReduceLogLevelWrapper(streams.log, Level.Debug, () => header(taskKeyLabel) + "aapt output: ")
          if (libraries.nonEmpty)
            streams.log.debug(header(taskKeyLabel) + "add libraries '" + libraries.map(_.pkgName).mkString("', '") + "'")
          val libraryResPathArgs = libraries.flatMap(_.resources.map(d => Seq("-S", d.absolutePath))).flatten
          val aapt = Seq(apPath.absolutePath, "package", "--auto-add-overlay", "-f", "--generate-dependencies",
            "-M", manPath.head.absolutePath,
            "-S", rPath.absolutePath) ++
            libraryResPathArgs ++
            Seq("-A", assetPath.absolutePath,
              "-I", jPath.absolutePath,
              "-F", resApkPath.absolutePath) ++
              (if (verbose) Seq("-v") else Seq())
          streams.log.debug(header(taskKeyLabel) + "packaging: " + aapt.mkString(" "))
          val code = Process(aapt) ! logger
          logger.flush(code)
          if (code != 0)
            sys.error(header(taskKeyLabel) + "error packaging resources")
          resApkPath
        }
    }
}

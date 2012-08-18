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

object AAPT extends MillStage {
  lazy val stagePrepareKey = aaptStagePrepare
  lazy val stageCoreKey = aaptStageCore
  lazy val stageFinalizerKey = aaptStageFinalizer
  val DefaultAAPTName = "aapt"
  val DefaultAAPTResourcesApkName = "resources.apk"

  val settings: Seq[Project.Setting[_]] = Seq(
    aaptAPKName := DefaultAAPTResourcesApkName,
    aaptAPKPath <<= (target, aaptAPKName)(_ / _),
    aaptName := DefaultAAPTName,
    aaptPackage <<= aaptPackageTask,
    aaptPath <<= (platformToolsPath, aaptName)(_ / _),
    aaptStagePrepare <<= stagePrepareTask,
    aaptStagePrepare <<= aaptStagePrepare dependsOn (preStagePrepare, makeManagedJavaPath),
    aaptStageCore <<= aaptStageCoreTask,
    aaptStageCore <<= aaptStageCore dependsOn aaptStagePrepare,
    aaptStageFinalizer <<= stageFinalizerTask,
    aaptStageFinalizer <<= aaptStageFinalizer dependsOn (aaptStagePrepare, aaptStageCore),
    sourceGenerators in Compile <+= aaptStageCore)

  def aaptStageCoreTask =
    (manifestPackage, aaptPath, manifestPath, mainResPath, jarPathSDK, managedJavaPath, libraries, streams) map {
      (mPackage, aPath, mPath, resPath, jPath, javaPath, libraries, s) =>
        task(s.log) {
          val libraryResPathArgs = for (
            lib <- libraries;
            d <- lib.resources.toSeq;
            arg <- Seq("-S", d.absolutePath)
          ) yield arg

          val libraryAssetPathArgs = for (
            lib <- libraries;
            d <- lib.assets.toSeq;
            arg <- Seq("-A", d.absolutePath)
          ) yield arg

          def runAapt(`package`: String, args: String*) {
            val aapt = Seq(aPath.absolutePath, "package", "--auto-add-overlay", "-m",
              "--custom-package", `package`,
              "-M", mPath.head.absolutePath,
              "-S", resPath.absolutePath,
              "-I", jPath.absolutePath,
              "-J", javaPath.absolutePath) ++
              args ++
              libraryResPathArgs ++
              libraryAssetPathArgs
            if (aapt.run(false).exitValue != 0) sys.error("error generating resources")
          }
          runAapt(mPackage)
          libraries.foreach(lib => runAapt(lib.pkgName, "--non-constant-id"))
          javaPath ** "R.java" get
        }
    }
  def aaptPackageTask: Project.Initialize[Task[File]] =
    (aaptPath, manifestPath, mainResPath, mainAssetsPath, jarPathSDK, aaptAPKPath, libraries, streams) map {
      (apPath, manPath, rPath, assetPath, jPath, resApkPath, apklibs, s) =>
        task(s.log, aaptPackage.key.label) {
          val libraryResPathArgs = for (
            lib <- apklibs;
            d <- lib.resources.toSeq;
            arg <- Seq("-S", d.absolutePath)
          ) yield arg

          val aapt = Seq(apPath.absolutePath, "package", "--auto-add-overlay", "-f",
            "-M", manPath.head.absolutePath,
            "-S", rPath.absolutePath,
            "-A", assetPath.absolutePath,
            "-I", jPath.absolutePath,
            "-F", resApkPath.absolutePath) ++
            libraryResPathArgs
          s.log.debug("packaging: " + aapt.mkString(" "))
          if (aapt.run(false).exitValue != 0) sys.error("error packaging resources")
          resApkPath
        }
    }
}

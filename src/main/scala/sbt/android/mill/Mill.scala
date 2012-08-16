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

import sbt._
import sbt.Keys._
import sbt.Plugin
import sbt.android.mill.MillKeys._
import stopwatch.StopwatchGroup
import java.lang.System
import scala.xml.XML

/**
 * main plugin class
 * task sequence related to http://developer.android.com/tools/building/index.html
 * before - pre
 * stage 1 - aidl
 * stage 2 - aapt
 * stage 3 - compile
 * stage 4 - proguard
 * stage 5 - dex
 * stage 6 - apkBuilder (aaptPackage)
 * stage 7 - jarSigner
 * stage 8 - zipAlign
 * stage 9 - publish
 */
abstract class Mill extends Plugin {
  val millSettings: Seq[Project.Setting[_]]
  def millConf = config("android-mill") extend (Compile)

  lazy val compositeSettings: Seq[Project.Setting[_]] = MillSettings.defaultSettings ++
    MillSettings.overwriteSettings ++
    MillSettings.runtimeSettings ++
    MillSettings.delegateSettings ++
    MillSettings.derivativeSettings ++
    Mill.settings ++
    Mill.tasksSequence

}

object Mill {
  @volatile var buildStartTime = 0L
  @volatile var profilingGroups = Seq[StopwatchGroup]()
  val tasksSequence = Seq(
    aaptStageCore <<= aaptStageCore dependsOn (preStageCore),
    aidlStageCore <<= aidlStageCore dependsOn (preStageCore),
    compileStageCorePre <<= compileStageCorePre dependsOn (aaptStageCore, aidlStageCore),
    packageConfig <<= packageConfigTask,
    proguardStageCore <<= proguardStageCore dependsOn (compileStageCore),
    aaptPackage <<= aaptPackage dependsOn (makeAssetPath, dxStageCore),
    deviceStageCore <<= deviceStageCore dependsOn packageDebug,
    emulatorStageCore <<= emulatorStageCore dependsOn packageDebug)
  /*
   * dxStageCore depends on dxInput that takes as input proguardStageCore
   * packageConfig depends on aaptPackage
   * packageDebug depends on packageConfigTask
   */

  val settings = Seq(
    cleanApk <<= (packageApkPath) map (IO.delete(_)),
    copyNativeLibraries <<= copyNativeLibrariesTask,
    libraries <<= dependenciesTask,
    librariesSources <<= dependenciesSourcesTask,
    makeAssetPath <<= directory(mainAssetsPath),
    makeManagedJavaPath <<= directory(managedJavaPath),
    packageDebug <<= packageTask(true),
    packageDebug <<= packageDebug dependsOn (cleanApk, aaptPackage, copyNativeLibraries),
    packageRelease <<= packageTask(false),
    packageRelease <<= packageRelease dependsOn (cleanApk, aaptPackage, copyNativeLibraries),
    sourceGenerators in Compile <+= librariesSources,
    statistics <<= statisticsTask)

  def copyNativeLibrariesTask =
    (streams, managedNativePath, dependencyClasspath in Compile) map {
      (s, natives, deps) =>
        {
          val sos = (deps.map(_.data)).filter(_.name endsWith ".so")
          var copied = Seq.empty[File]
          for (so <- sos)
            getNativeTarget(natives, so.name, "armeabi") orElse getNativeTarget(natives, so.name, "armeabi-v7a") map {
              target =>
                target.getParentFile.mkdirs
                IO.copyFile(so, target)
                copied +:= target
                s.log.debug("copied native:   " + target.toString)
            }
          /* clean up stale native libraries */
          for (path <- IO.listFiles(natives / "armeabi") ++ IO.listFiles(natives / "armeabi-v7a")) {
            s.log.debug("checking native: " + path.toString)
            if (path.name.endsWith(".so") && !copied.contains(path)) {
              IO.delete(path)
              s.log.debug("deleted native:  " + path.toString)
            }
          }
        }
    }
  def dependenciesTask =
    (update in Compile, sourceManaged, managedJavaPath, resourceManaged,
      assetsDirectoryName, resDirectoryName, manifestName, streams) map {
        (updateReport, srcManaged, javaManaged, resManaged, resName, assetsName, manifestName, s) =>
          stopwatchGroup(MillKeys.libraries.key.label) {
            val libraries = updateReport.matching(artifactFilter(`type` = "apklib"))
            libraries map { apklib =>
              s.log.info("extracting library " + apklib.name)
              val dest = srcManaged / ".." / apklib.base

              val unzipped = IO.unzip(apklib, dest)
              def moveContents(fromDir: File, toDir: File) = {
                toDir.mkdirs()
                val pairs = for (
                  file <- unzipped;
                  rel <- IO.relativize(fromDir, file)
                ) yield (file, toDir / rel)
                IO.move(pairs)
                pairs map { case (_, t) => t }
              }
              val sources = moveContents(dest / "src", javaManaged)

              val manifest = dest / manifestName
              val pkgName = XML.loadFile(manifest).attribute("package").get.head.text
              LibraryProject(
                pkgName,
                manifest,
                sources,
                Some(dest / resName) filter { _.exists },
                Some(dest / assetsName) filter { _.exists })
            }
          }
      }
  def dependenciesSourcesTask =
    (libraries, streams) map {
      (projectLibs, s) =>
        stopwatchGroup(MillKeys.librariesSources.key.label) {
          if (!projectLibs.isEmpty) {
            s.log.debug("generating source files from apklibs")
            val xs = for (
              l <- projectLibs;
              f <- l.sources
            ) yield f

            s.log.info("generated " + xs.size + " source files from " + projectLibs.size + " apklibs")
            xs
          } else Seq.empty
        }
    }
  def packageConfigTask =
    (toolsPath, packageApkPath, aaptAPKPath, dxProjectDexPath,
      nativeLibrariesPath, managedNativePath, dxInputs, resourceDirectory) map
      (ApkConfig(_, _, _, _, _, _, _, _))
  def packageTask(debug: Boolean): Project.Initialize[Task[File]] = (packageConfig, streams) map { (c, s) =>
    val builder = new MillApkBuilder(c, debug)
    builder.build.fold(sys.error(_), s.log.info(_))
    s.log.debug(builder.outputStream.toString)
    c.packageApkPath
  }
  def statisticsTask = (streams) map (s => dumpStopWatchStatistics(s.log))
  /*
   * helpers
   */
  def determineAndroidSdkPath(es: Seq[String]) = {
    val paths = for (e <- es; p = System.getenv(e); if p != null) yield p
    if (paths.isEmpty) None else Some(Path(paths.head).asFile)
  }
  def directory(path: SettingKey[File]) = path map (IO.createDirectory(_))
  def dumpStopWatchStatistics(implicit log: Logger) = profilingGroups.sortBy(_.name).foreach {
    group =>
      log.info("""process "%s" stopwatch statistics:""".format(group.name))
      group.names.toSeq.sorted.foreach(name =>
        log.info("  " + group.snapshot(name).toShortString))
  }
  def getNativeTarget(parent: File, name: String, abi: String) = {
    val extension = "-" + abi + ".so"
    if (name endsWith extension) {
      val stripped = name.substring(0, name indexOf '-') + ".so"
      val target = new File(abi) / stripped
      Some(parent / target.toString)
    } else None
  }
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
  def isWindows = System.getProperty("os.name").startsWith("Windows")
  def manifest(mpath: File) = xml.XML.loadFile(mpath)
  def osBatchSuffix = if (isWindows) ".bat" else ""
  def stopwatchGroup = Mill.getStopwatchGroup("core")
}

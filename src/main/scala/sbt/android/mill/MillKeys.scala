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
import sbt.inc.Analysis

object MillKeys {
  // core sbt.android.mill
  val statistics = TaskKey[Unit]("statistics", "Show execution statistics")
  // stage1 sbt.android.mill.pre
  val preStagePrepare = TaskKey[Unit]("pre-prepare", "First task in sequence before build project.")
  val preStageCore = TaskKey[Unit]("pre-core", "Core task for preparing environment before build project.")
  val preStageFinalizer = TaskKey[Unit]("pre", "Prepage environment before build project.")
  // stage2 sbt.android.mill.aidl
  val aidlName = SettingKey[String]("aidl-name")
  val aidlPath = SettingKey[File]("aidl-path")
  val aidlStagePrepare = TaskKey[Unit]("aidl-prepare", "Prepare for generation Java classes from .aidl files.")
  val aidlStageCore = TaskKey[Seq[File]]("aidl-core", "Core task for generation Java classes from .aidl files.")
  val aidlStageFinalizer = TaskKey[Unit]("aidl-generate", "Generate Java classes from .aidl files.")
  // stage3 sbt.android.mill.aapt
  val aaptName = SettingKey[String]("aapt-name")
  val aaptPath = SettingKey[File]("aapt-path")
  val aaptAPKName = SettingKey[String]("aapt-apk-name", "AAPT output file name")
  val aaptAPKPath = SettingKey[File]("aapt-apk-path", "AAPT output file path")
  val aaptStagePrepare = TaskKey[Unit]("aapt-prepare", "Prepare for generation R.java.")
  val aaptStageCore = TaskKey[Seq[File]]("aapt-core", "Core task for generation R.java.")
  val aaptStageFinalizer = TaskKey[Unit]("aapt-generate", "Generate R.java.")
  // stage4 sbt.android.mill.compile
  val compileStagePrepare = TaskKey[Unit]("compile-prepare", "Prepare for compilation and compilation.")
  val compileStageCorePre = TaskKey[Unit]("compile-core-pre", "Core task for compilation. Setup statistics.")
  val compileStageCore = TaskKey[Unit]("compile-core", "Core task for compilation. Gathering statistics.")
  val compileStageFinalizer = TaskKey[Unit]("compile-project", "Compilate project.")
  // stage5 sbt.android.mill.proguard
  val proguardEnabled = SettingKey[Boolean]("proguard-enabled")
  val proguardExclude = TaskKey[Seq[File]]("proguard-exclude")
  val proguardInJars = TaskKey[Seq[File]]("proguard-in-jars")
  val proguardOption = TaskKey[Seq[String]]("proguard-option")
  val proguardOptimizations = SettingKey[Seq[String]]("proguard-optimizations")
  val proguardProjectJarName = SettingKey[String]("proguard-project-jar-name")
  val proguardProjectJarPath = SettingKey[File]("classes-min-jar-path")
  val proguardStagePrepare = TaskKey[Unit]("proguard-prepare", "Prepare for optimization class files.")
  val proguardStageCore = TaskKey[Option[File]]("proguard-core", "Core task for optimization class files.")
  val proguardStageFinalizer = TaskKey[Unit]("proguard-shrink", "Optimize class files.")

  /** Names */
  val assetsDirectoryName = SettingKey[String]("assets-dir-name")
  val jarNameSDK = SettingKey[String]("sdk-jar-name", "Name of SDK library")
  val manifestName = SettingKey[String]("manifest-name", "The manifest presents essential information about the application to the Android system")
  val platformName = SettingKey[String]("platform-name", "Targetted android platform")
  val resDirectoryName = SettingKey[String]("res-dir-name")
  val manifestPackageName = TaskKey[String]("manifest-package-name")

  /** Path Settings */
  val jarPathSDK = SettingKey[File]("sdk-jar-path", "Path to SDK library")
  val mainAssetsPath = SettingKey[File]("main-asset-path")
  val mainResPath = TaskKey[File]("main-res-path")
  val managedScalaPath = SettingKey[File]("managed-scala-path")
  val managedJavaPath = SettingKey[File]("managed-java-path")
  val managedNativePath = SettingKey[File]("managed-native-path")
  val manifestPackagePath = TaskKey[String]("manifest-package")
  val manifestPath = TaskKey[Seq[File]]("manifest-path", "Path to AndroidManifest.xml")
  val nativeLibrariesPath = SettingKey[File]("natives-lib-path")
  val platformPath = SettingKey[File]("platform-path")
  val platformToolsPath = SettingKey[File]("platform-tools-path")
  val sdkPath = SettingKey[File]("sdk-path")

  /** Base Settings */
  val envs = SettingKey[Seq[String]]("envs")
  val libraries = TaskKey[Seq[LibraryProject]]("libraries", "Library projects")
  val librariesSources = TaskKey[Seq[File]]("libraries-sources", "Enumerate Java sources from library projects")
  val makeManagedJavaPath = TaskKey[Unit]("make-managed-java-path")
  val preinstalledModules = SettingKey[Seq[ModuleID]]("preinstalled-modules", "A list of modules which are already included in Android")

  case class LibraryProject(pkgName: String, manifest: File, sources: Set[File], resources: Option[File], assets: Option[File])
}
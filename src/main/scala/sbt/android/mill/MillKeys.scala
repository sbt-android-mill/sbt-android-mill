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
import sbt.inc.Analysis

object MillKeys {
  def millConf = config("android-mill") extend (Compile)

  // core sbt.android.mill
  val statistics = TaskKey[Unit]("statistics", "Show execution statistics")
  val statisticsReset = SettingKey[Boolean]("statistics-reset", "Reset profiling statistics before new task sequence")
  // stage1 sbt.android.mill.pre
  val preStagePrepare = TaskKey[Unit]("pre-prepare", "First task in sequence before build project.")
  val preStageCore = TaskKey[Unit]("pre-core", "Core task for preparing environment before build project.")
  val preStageFinalizer = TaskKey[Unit]("pre", "Prepare environment before build project.")
  // stage2 sbt.android.mill.aidl
  val aidlName = SettingKey[String]("aidl-name")
  val aidlPath = SettingKey[File]("aidl-path")
  val aidlStagePrepare = TaskKey[Unit]("aidl-prepare", "Prepare for generation Java classes from .aidl files.")
  val aidlStageCore = TaskKey[Seq[File]]("aidl-core", "Core task for generation Java classes from .aidl files.")
  val aidlStageFinalizer = TaskKey[Unit]("aidl-generate", "Generate Java classes from .aidl files.")
  // stage3 sbt.android.mill.aapt
  val aaptApkName = SettingKey[String]("aapt-apk-name", "aapt output file name")
  val aaptApkPath = SettingKey[File]("aapt-apk-path", "aapt output file path")
  val aaptName = SettingKey[String]("aapt-name")
  val aaptPackage = TaskKey[File]("aapt-package", "Package resources and assets.")
  val aaptPath = SettingKey[File]("aapt-path")
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
  // stage6 sbt.android.mill.dx
  val dxName = SettingKey[String]("dx-name")
  val dxInputs = TaskKey[Seq[File]]("dx-inputs", "Input for dex command")
  val dxOpts = SettingKey[Tuple2[String, Option[Seq[String]]]]("dx-opts")
  val dxPath = SettingKey[File]("dx-path")
  val dxProjectDexName = SettingKey[String]("dx-project-dex-name")
  val dxProjectDexPath = SettingKey[File]("dx-project-dex-path")
  val dxStagePrepare = TaskKey[Unit]("dx-prepare", "Prepare for conversion class files.")
  val dxStageCore = TaskKey[File]("dx-core", "Core task for conversion class files.")
  val dxStageFinalizer = TaskKey[Unit]("dx", "Convert class files to dex file.")
  /*
   * step 7 aaptPackage
   * step 8 packageTask
   */
  // sbt.android.mill.target
  val targetADBName = SettingKey[String]("target-adb-name")
  val targetADBPath = SettingKey[File]("target-adb-path")
  val targetADBTimeout = SettingKey[Int]("target-connection-timeout", "Time in milliseconds for 'adb wait-for-device' and related functions.")
  val targetADBLogLevel = SettingKey[Level.Value]("target-log-level", "Maximum adb log level.")
  val targetReinstallKeep = SettingKey[Boolean]("target-reinstall-keep", "Keep the data and cache directories while reinstall.")
  // sbt.android.mill.target.Device
  val deviceStagePrepare = TaskKey[Unit]("device-install-prepare", "Prepare for installation debug package on device.")
  val deviceStageCore = TaskKey[Unit]("device-install-core", "Core task for installation debug package on device.")
  val deviceStageFinalizer = TaskKey[Unit]("device-install", "Install debug package on device.")
  val deviceStageUninstall = TaskKey[Unit]("device-uninstall", "Uninstall package on device.")
  val deviceStageUninstallSoft = TaskKey[Unit]("device-uninstall-soft", "Uninstall package on device, keep the data and cache directories.")
  val deviceStageReinstall = TaskKey[Unit]("device-reinstall", "Uninstall package on device before installation process.")  
  val deviceTest = TaskKey[Unit]("device-test", "Runs tests on device.")
  val deviceTestOnly = InputKey[Unit]("device-test-only", "Run a single test on device")
  // sbt.android.mill.target.Emulator
  val emulatorStagePrepare = TaskKey[Unit]("emulator-install-prepare", "Prepare for installation debug package on emulator.")
  val emulatorStageCore = TaskKey[Unit]("emulator-install-core", "Core task for installation debug package on emulator.")
  val emulatorStageFinalizer = TaskKey[Unit]("emulator-install", "Install debug package on emulator.")
  val emulatorStageUninstall = TaskKey[Unit]("emulator-uninstall", "Uninstall package on emulator.")
  val emulatorStageUninstallSoft = TaskKey[Unit]("emulator-uninstall-soft", "Uninstall package on emulator, keep the data and cache directories.")
  val emulatorStageReinstall = TaskKey[Unit]("emulator-reinstall", "Uninstall package on emulator before installation process.")  
  val emulatorTest = TaskKey[Unit]("emulator-test", "Runs tests in emulator.")
  val emulatorTestOnly = InputKey[Unit]("emulator-test-only", "Run a single test on emulator")
  // sbt.android.mill.ndk
  val ndkBasePath = SettingKey[File]("ndk-output-path", "Base path for build process where 'jni' folder exists and optionaly 'obj', 'libs'")
  val ndkBuildName = SettingKey[String]("ndk-build-name", "Name for the 'ndk-build' tool")
  val ndkBuildPath = SettingKey[File]("ndk-build-path", "Path to the 'ndk-build' tool")
  val ndkClean = TaskKey[Unit]("ndk-clean", "Clean resources built from native C/C++ sources.")
  val ndkEnvs = SettingKey[Seq[String]]("ndk-envs", "List of environment variables to check for the NDK.")
  val ndkJavah = TaskKey[Unit]("ndk-javah", "Produce C headers from Java classes with native methods.")
  val ndkJavahClean = TaskKey[Unit]("ndk-javah-clean", "Clean C headers built from Java classes with native methods.")
  val ndkJavahName = SettingKey[String]("ndk-javah-name", "The name of the javah command for generating JNI headers.")
  val ndkJavahPath = SettingKey[String]("ndk-javah-path", "The path to the javah executable.")
  val ndkJavahOutputDirectory = SettingKey[File]("ndk-javah-output-directory", "The directory where JNI headers are written to.")
  val ndkJavahOutputFile = SettingKey[Option[File]]("ndk-javah-output-file", "Filename for the generated C header, relative to javah-output-directory.")
  val ndkJavahOutputEnv = SettingKey[String]("ndk-javah-output-env", "Name of the make environment variable to bind to the javah-output-directory.")
  val ndkJNIClasses = SettingKey[Seq[String]]("ndk-jni-classes", "Fully qualified names of classes with native methods for which JNI headers are to be generated.")
  val ndkJNIDirectoryName = SettingKey[String]("ndk-jni-directory-name", "Directory name for native sources.")
  val ndkJNIDirectoryPath = SettingKey[File]("ndk-jni-directory-path", "Path to native sources. (with Android.mk)")
  val ndkObjectDirectoryName = SettingKey[String]("ndk-object-name", "Directory name for compiled native objects.")
  val ndkObjectDirectoryPath = SettingKey[File]("ndk-object-path")
  val ndkStagePrepare = TaskKey[Unit]("ndk-prepare", "Prepare for compilation native C/C++ sources.")
  val ndkStageCore = TaskKey[Unit]("ndk-core", "Core task for compilation native C/C++ sources.")
  val ndkStageFinalizer = TaskKey[Unit]("ndk-build", "Compile native C/C++ sources.")

  /** Names */
  val assetsDirectoryName = SettingKey[String]("assets-dir-name")
  val jarNameSDK = SettingKey[String]("sdk-jar-name", "Name of SDK library")
  val manifestName = SettingKey[String]("manifest-name", "The manifest presents essential information about the application to the Android system")
  val manifestPackageName = TaskKey[String]("manifest-package-name")
  val packageApkName = TaskKey[String]("package-apk-name")
  val platformName = SettingKey[String]("platform-name", "Targetted android platform")
  val resDirectoryName = SettingKey[String]("res-dir-name")

  /** Path Settings */
  val jarPathSDK = SettingKey[File]("sdk-jar-path", "Path to SDK library")
  val mainAssetsPath = SettingKey[File]("main-asset-path")
  val mainResPath = TaskKey[File]("main-res-path")
  val managedScalaPath = SettingKey[File]("managed-scala-path")
  val managedJavaPath = SettingKey[File]("managed-java-path")
  val managedNativePath = SettingKey[File]("managed-native-path")
  val manifestPackage = TaskKey[String]("manifest-package")
  val manifestPath = TaskKey[Seq[File]]("manifest-path", "Path to AndroidManifest.xml")
  val nativeLibrariesPath = SettingKey[File]("natives-lib-path")
  val packageApkPath = TaskKey[File]("package-apk-path")
  val platformPath = SettingKey[File]("platform-path")
  val platformToolsPath = SettingKey[File]("platform-tools-path")
  val sdkPath = SettingKey[File]("sdk-path")
  val toolsPath = SettingKey[File]("tools-path")

  /** Base Settings */
  val envs = SettingKey[Seq[String]]("envs")
  val libraries = TaskKey[Seq[LibraryProject]]("libraries", "Library projects")
  val librariesSources = TaskKey[Seq[File]]("libraries-sources", "Enumerate Java sources from library projects")
  val manifestSchema = SettingKey[String]("manifest-schema")
  val makeAssetPath = TaskKey[Unit]("make-assest-path")
  val makeManagedJavaPath = TaskKey[Unit]("make-managed-java-path")
  val packageConfig = TaskKey[ApkConfig]("package-config", "Generates an apk config")
  val preinstalledModules = SettingKey[Seq[ModuleID]]("preinstalled-modules", "A list of modules which are already included in Android")
  val versionName = TaskKey[String]("version-name")

  /** Base Tasks */
  val cleanApk = TaskKey[Unit]("clean-apk", "Remove apk package.")
  val copyNativeLibraries = TaskKey[Unit]("copy-native-libraries", "Copy native libraries added to libraries.")
  val packageDebugCore = TaskKey[File]("package-debug-core", "Core task for oackage and sign with a debug key.")
  val packageDebug = TaskKey[Unit]("package-debug", "Package and sign with a debug key.")
  val packageReleaseCore = TaskKey[File]("package-release-core", "Core task for oackage without signing.")
  val packageRelease = TaskKey[Unit]("package-release", "Package without signing.")

  // Replaces the Installable argument
  case class ApkConfig(
    androidToolsPath: File,
    packageApkPath: File,
    resourcesApkPath: File,
    classesDexPath: File,
    nativeLibrariesPath: File,
    managedNativePath: File,
    dexInputs: Seq[File],
    resourceDirectory: File)

  case class LibraryProject(pkgName: String, manifest: File, sources: Set[File], resources: Option[File], assets: Option[File])
}

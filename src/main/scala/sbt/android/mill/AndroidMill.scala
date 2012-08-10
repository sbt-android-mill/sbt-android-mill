package sbt.android.mill

import sbt._
import Keys._
import sbt.Plugin
import sbt.android.mill.AndroidKeys._

/**
 * main plugin class
 * http://developer.android.com/tools/building/index.html
 * before - pre
 * stage 1 - aidl
 * stage 2 - aapt
 * stage 3 - compile
 * stage 4 - dex
 * stage 5 - apkBuilder
 * stage 6 - jarSigner
 * stage 7 - zipAlign
 * stage 8 - publish
 * after - post
 */
abstract class AndroidMill extends Plugin {
  val millSettings: Seq[Project.Setting[_]]
  def millConf = config("android-mill") extend (Compile)

  val DefaultEnvs = List("ANDROID_SDK_HOME", "ANDROID_SDK_ROOT", "ANDROID_HOME")
  val DefaultAssetsDirectoryName = "assets"
  val DefaultResDirectoryName = "res"

  val defaultSettings = Seq[Setting[_]](
    assetsDirectoryName := DefaultAssetsDirectoryName,
    resDirectoryName := DefaultResDirectoryName,
    envs := DefaultEnvs)
  val runtimeSettings = Seq[Setting[_]](
    sdkPath <<= (envs, baseDirectory) { (es, dir) =>
      AndroidHelpers.determineAndroidSdkPath(es).getOrElse {
        val local = new File(dir, "local.properties")
        if (local.exists()) {
          (for (
            sdkDir <- (for (
              l <- IO.readLines(local);
              if (l.startsWith("sdk.dir"))
            ) yield l.substring(l.indexOf('=') + 1))
          ) yield new File(sdkDir)).headOption.getOrElse(
            sys.error("local.properties did not contain sdk.dir"))
        } else sys.error(
          "Android SDK not found. You might need to set %s".format(es.mkString(" or ")))
      }
    },
    platformToolsPath <<= (sdkPath)(_ / "platform-tools"))
  val delegateSettings = Seq[Setting[_]](
    // Handle the delegates for android settings
    classDirectory <<= (classDirectory in Compile),
    sourceDirectory <<= (sourceDirectory in Compile),
    sourceDirectories <<= (sourceDirectories in Compile),
    resourceDirectory <<= (resourceDirectory in Compile),
    resourceDirectories <<= (resourceDirectories in Compile),
    javaSource <<= (javaSource in Compile),
    managedClasspath <<= (managedClasspath in Runtime),
    fullClasspath <<= (fullClasspath in Runtime))
  val derivativeSettings = Seq[Setting[_]](
    nativeLibrariesPath <<= (sourceDirectory)(_ / "libs"),
    mainAssetsPath <<= (sourceDirectory, assetsDirectoryName)(_ / _),
    mainResPath <<= (sourceDirectory, resDirectoryName)(_ / _) map (x => x),
    managedJavaPath <<= (sourceManaged in Compile)(_ / "java"),
    managedScalaPath <<= (sourceManaged in Compile)(_ / "scala"),
    managedNativePath <<= (sourceManaged in Compile)(_ / "native_libs"),
    platformPath <<= (sdkPath, platformName) (_ / "platforms" / _))
  val commonSettings = defaultSettings ++ runtimeSettings ++ delegateSettings ++ derivativeSettings
}

object AndroidMill {
  val profiling = new stopwatch.StopwatchGroup("android-mill")
}
/*
trait AndroidFabric extends Plugin with Stage1Main with Stage2Main with Stage3Main with Stage4Main {
  /** Path Settings */

  val stage5_dex = TaskKey[Unit]("stage5_dex")
  val stage6_apkBuilder = TaskKey[Unit]("stage6_apkBuilder")
  val stage7_jarSigner = TaskKey[Unit]("stage7_jarSigner")
  val stage8_zipAlign = TaskKey[Unit]("stage8_zipAlign")
  val stage9_publish = TaskKey[Unit]("stage9_publish")

  val fabricSettings = inConfig(fabricConf)(Seq(
    stage1_prepare <<= stage1_prepareTask,
    stage2_AAPT <<= stage2_AAPTTask,
    stage2_AAPT <<= stage2_AAPT dependsOn (stage1_prepare),
    stage3_aidl <<= stage3_aidlTask,
    stage3_aidl <<= stage3_aidl dependsOn (stage1_prepare),
    stage4_compile <<= stage4_compileTask,
    stage4_compile <<= stage4_compile dependsOn (stage2_AAPT, stage3_aidl))) ++
    AndroidFabric.commonSettings ++
    settingsStage3
}

*/
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

package sbt.android.mill.ndk

import sbt._
import sbt.Keys._
import sbt.android.mill.MillKeys._
import sbt.android.mill.Mill
import java.io.File
import sbt.android.mill.MillStage
import sbt.android.mill.util.ReduceLogLevelWrapper

/**
 * Support for the Android NDK.
 *
 * Adding support for compilation of C/C++ sources using the NDK.
 *
 * Adapted from work by Daniel Solano Gómez, Martin Kneissl
 *
 * @author Daniel Solano Gómez, Martin Kneissl, Alexey Aksenov.
 */
object NDK extends MillStage {
  lazy val stagePrepareKey = ndkStagePrepare
  lazy val stageCoreKey = ndkStageCore
  lazy val stageFinalizerKey = ndkStageFinalizer
  /** The default name for the 'ndk-build' tool. */
  val DefaultNdkBuildName = "ndk-build"
  /** The default name for the 'javah' tool. */
  val DefaultNdkJavahName = "javah"
  /** The default directory name for native sources. */
  val DefaultJniDirectoryName = "jni"
  /** The default directory name for compiled native objects. */
  val DefaultObjectDirectoryName = "obj"
  /** The list of environment variables to check for the NDK. */
  val DefaultEnvs = List("ANDROID_NDK_HOME", "ANDROID_NDK_ROOT")
  /** The make environment variable name for the javah generated header directory. */
  val DefaultJavahOutputEnv = "SBT_MANAGED_JNI_INCLUDE"

  val settings = Seq(
    cleanFiles <+= ndkObjectDirectoryPath,
    ndkBuildName := DefaultNdkBuildName,
    ndkBuildPath <<= ndkBuildPathTask,
    ndkJNIDirectoryName := DefaultJniDirectoryName,
    ndkJNIDirectoryPath <<= (sourceDirectory, ndkJNIDirectoryName)(_ / _),
    ndkObjectDirectoryName := DefaultObjectDirectoryName,
    ndkObjectDirectoryPath <<= (ndkBasePath, ndkObjectDirectoryName)(_ / _),
    ndkBasePath <<= (ndkJNIDirectoryPath)(_.getParentFile),
    ndkEnvs := DefaultEnvs,
    ndkJavah <<= javahTask,
    ndkJavah <<= ndkJavah dependsOn (compile in Compile),
    ndkJavahClean <<= (ndkJavahOutputDirectory) map IO.delete,
    ndkJavahName := DefaultNdkJavahName,
    ndkJavahPath <<= (javaHome, ndkJavahName)((home, name) =>
      home map (h => (h / "bin" / name).absolutePath) getOrElse name),
    ndkJavahOutputEnv := DefaultJavahOutputEnv,
    ndkJavahOutputFile := None,
    ndkJavahOutputDirectory <<= (sourceManaged, ndkJNIDirectoryName)((path, name) => path / "main" / name),
    ndkClean <<= ndkStageCoreTask("clean"),
    ndkJNIClasses := Seq.empty,
    ndkClean <<= ndkClean dependsOn (ndkJavahClean),
    ndkStagePrepare <<= stagePrepareTask,
    ndkStagePrepare <<= ndkStagePrepare dependsOn (preStagePrepare),
    ndkStageCore <<= ndkStageCoreTask(),
    ndkStageCore <<= ndkStageCore dependsOn (ndkJavah, ndkStagePrepare),
    ndkStageFinalizer <<= stageFinalizerTask,
    ndkStageFinalizer <<= ndkStageFinalizer dependsOn (ndkStagePrepare, ndkStageCore),
    products in Compile <<= products in Compile dependsOn (ndkStageCore))
  // TODO register source generator
  def ndkBuildPathTask =
    (ndkEnvs, ndkBuildName) { (envs, ndkBuildName) =>
      val paths = for {
        e <- envs
        p = System.getenv(e)
        if p != null
        b = new File(p, ndkBuildName)
        if b.canExecute
      } yield b
      paths.headOption getOrElse (sys.error("Android NDK not found.  " +
        "You might need to set " + envs.mkString(" or ")))
    }

  def ndkStageCoreTask(targets: String*): Project.Initialize[Task[Unit]] =
    (ndkBuildPath, ndkJavahOutputEnv, ndkJavahOutputDirectory, ndkJNIDirectoryPath, ndkBasePath, streams) map {
      (ndkBuildPath, javahOutputEnv, javahOutputDirectory, jniPath, basePath, streams) =>
        val tag = targets.headOption match {
          case Some("clean") => ndkClean.key.label
          case _ => this.tag
        }
        if (jniPath.exists)
          task(streams.log, tag) {
            val logger = new ReduceLogLevelWrapper(streams.log, Level.Debug, () => header() + "ndk output: ")
            val ndkBuild = ndkBuildPath.absolutePath :: "-C" :: basePath.absolutePath ::
              (javahOutputEnv + "=" + javahOutputDirectory.absolutePath) :: targets.toList
            streams.log.debug(header(tag) + "running ndk-build: " + ndkBuild.mkString(" "))
            val code = Process(ndkBuild) ! logger
            logger.flush(code)
            if (code != 0)
              streams.log.error(header(tag) + "ndk-build failed with nonzero exit code (" + code + ")")
          }
        else
          streams.log.debug(header(tag) + "ndk-jni-directory-path not exists, skip")
    }
  def javahTask =
    (ndkJavahPath, classDirectory in Compile, internalDependencyClasspath in Compile,
      externalDependencyClasspath in Compile, ndkJNIClasses, ndkJavahOutputDirectory, ndkJavahOutputFile, streams) map {
        (javahPath, classDirectory, internalDependencyClasspath, externalDependencyClasspath,
        classes, outputDirectory, outputFile, streams) =>
          val taskKeyLabel = ndkJavah.key.label
          val classpath = Seq(classDirectory) ++ internalDependencyClasspath.files ++ externalDependencyClasspath.files
          val log = streams.log
          if (classes.isEmpty) {
            log.debug(header(taskKeyLabel) + "no JNI classes, skip")
          } else {
            outputDirectory.mkdirs()
            val classpathArgument = classpath.map(_.getAbsolutePath()).mkString(File.pathSeparator)
            val outputArguments = outputFile match {
              case Some(file) =>
                val outputFile = compose(outputDirectory, file)
                // Neither javah nor RichFile.relativeTo will work unless the directories exist.
                Option(outputFile.getParentFile) foreach (_.mkdirs())
                if (!(outputFile relativeTo outputDirectory).isDefined) {
                  log.warn(header(taskKeyLabel) + "javah output file [" + outputFile + "] is not within javah output directory [" +
                    outputDirectory + "], continuing anyway")
                }
                Seq("-o", outputFile.absolutePath)
              case None => Seq("-d", outputDirectory.absolutePath)
            }
            val javahCommandLine = Seq(javahPath, "-classpath", classpathArgument) ++ outputArguments ++ classes
            log.debug(header(taskKeyLabel) + "running javah: " + (javahCommandLine mkString " "))
            val exitCode = Process(javahCommandLine) ! log
            if (exitCode != 0)
              sys.error(header(taskKeyLabel) + "javah exited with " + exitCode)
          }
      }
  private def compose(parent: File, child: File): File =
    if (child.isAbsolute) child else split(child).foldLeft(parent)(new File(_, _))
  private def split(file: File) = {
    val parentsBottomToTop = Iterator.iterate(file)(_.getParentFile).takeWhile(_ != null).map(_.getName).toSeq
    parentsBottomToTop.reverse
  }
}

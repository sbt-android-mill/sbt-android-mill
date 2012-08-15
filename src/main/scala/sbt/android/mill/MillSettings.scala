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

import scala.xml.NodeSeq.seqToNodeSeq

import MillKeys._
import sbinary.DefaultProtocol.StringFormat
import sbt._
import sbt.Keys._

object MillSettings {
  val defaultSettings = Seq[Setting[_]](
    assetsDirectoryName := MillDefaults.assetsDirectoryName,
    envs := MillDefaults.envs,
    jarNameSDK := MillDefaults.jarNameSDK,
    manifestName := MillDefaults.manifestName,
    preinstalledModules := MillDefaults.preinstalledModules,
    resDirectoryName := MillDefaults.resDirectoryName)
  val runtimeSettings = Seq[Setting[_]](
    manifestPackagePath <<= findManifestPath,
    // after task manifestPath completed fire triggeredBy callback
    manifestPackageName <<= findManifestPath storeAs manifestPackageName triggeredBy manifestPath,
    platformToolsPath <<= (sdkPath)(_ / "platform-tools"),
    sdkPath <<= (envs, baseDirectory) { (es, dir) =>
      MillHelpers.determineAndroidSdkPath(es).getOrElse {
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
    })
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
    jarPathSDK <<= (platformPath, jarNameSDK)(_ / _),
    mainAssetsPath <<= (sourceDirectory, assetsDirectoryName)(_ / _),
    mainResPath <<= (sourceDirectory, resDirectoryName)(_ / _) map (x => x),
    managedJavaPath <<= (sourceManaged in Compile)(_ / "java"),
    managedScalaPath <<= (sourceManaged in Compile)(_ / "scala"),
    managedNativePath <<= (sourceManaged in Compile)(_ / "native_libs"),
    manifestPath <<= (sourceDirectory, manifestName) map ((s, m) => Seq(s / m)),
    nativeLibrariesPath <<= (sourceDirectory)(_ / "libs"),
    platformPath <<= (sdkPath, platformName)(_ / "platforms" / _))
  val overwriteSettings = Seq[Setting[_]](
    managedSourceDirectories in Compile <<= (managedJavaPath, managedScalaPath)(Seq(_, _)),
    unmanagedJars in Compile <++= (jarPathSDK) map (_.get.map(Attributed.blank(_))),
    classpathTypes in Compile := Set("jar", "bundle", "so"))

  def findManifestPath() = (manifestPath) map (p =>
    MillHelpers.manifest(p.head).attribute("package").getOrElse(sys.error("package not defined")).text)
}

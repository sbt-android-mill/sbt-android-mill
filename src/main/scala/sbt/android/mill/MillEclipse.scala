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
import sbt.android.mill.MillKeys._

object MillEclipse extends Mill {
  override def projectSettings = MillClassic.projectSettings ++ inConfig(MillKeys.millConf)(Seq(
    manifestPath <<= (baseDirectory, manifestName) map ((base, name) => Seq(base / name)),
    mainAssetsPath <<= (baseDirectory, assetsDirectoryName)(_ / _),
    mainResPath <<= (baseDirectory, resDirectoryName) map (_ / _),
    managedJavaPath <<= (baseDirectory)(_ / "gen"),
    nativeLibrariesPath <<= (sourceDirectory)(_ / "libs"),
    aaptApkName := "resources.ap_",
    aaptApkPath <<= (crossTarget, aaptApkName)(_ / _),
    dxProjectDexPath <<= (crossTarget, dxProjectDexName)(_ / _),
    packageApkName <<= (name) map ((a) => String.format("%s.apk", a)),
    packageApkPath <<= (crossTarget, packageApkName) map (_ / _),
    crossTarget <<= (baseDirectory)(_ / "bin"),
    Keys.target <<= (baseDirectory)(_ / "bin"))) ++
    Seq(crossTarget <<= (baseDirectory)(_ / "bin"),
      Keys.target <<= (baseDirectory)(_ / "bin"))
}

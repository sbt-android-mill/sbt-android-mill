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

object MillDefaults {
  val assetsDirectoryName = "assets"
  val envs = List("ANDROID_SDK_HOME", "ANDROID_SDK_ROOT", "ANDROID_HOME")
  val jarNameSDK = "android.jar"
  val manifestName = "AndroidManifest.xml"
  val manifestSchema = "http://schemas.android.com/apk/res/android"
  val preinstalledModules = Seq[ModuleID](
    ModuleID("org.apache.httpcomponents", "httpcore", null),
    ModuleID("org.apache.httpcomponents", "httpclient", null),
    ModuleID("org.json", "json", null),
    ModuleID("commons-logging", "commons-logging", null),
    ModuleID("commons-codec", "commons-codec", null))
  val resDirectoryName = "res"
}

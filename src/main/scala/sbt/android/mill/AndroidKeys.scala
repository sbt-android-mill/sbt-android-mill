/**
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

object AndroidKeys {
  // sbt.android.mill.aidl
  val aidlName = SettingKey[String]("aidl-name")
  val aidlPath = SettingKey[File]("aidl-path")
  val aidlBefore = TaskKey[Boolean]("aidl-before", "Before hook for generate Java classes from .aidl files.")
  val aidlStage = TaskKey[Seq[File]]("aidl-generate", "Generate Java classes from .aidl files.")
  val aidlAfter = TaskKey[Unit]("aidl-after", "After hook for generate Java classes from .aidl files.")

  /** Names */
  val platformName = SettingKey[String]("platform-name", "Targetted android platform")
  val assetsDirectoryName = SettingKey[String]("assets-dir-name")
  val resDirectoryName = SettingKey[String]("res-dir-name")

  /** Path Settings */
  val sdkPath = SettingKey[File]("sdk-path")
  val platformPath = SettingKey[File]("platform-path")
  val platformToolsPath = SettingKey[File]("platform-tools-path")
  val nativeLibrariesPath = SettingKey[File]("natives-lib-path")
  val mainAssetsPath = SettingKey[File]("main-asset-path")
  val mainResPath = TaskKey[File]("main-res-path")
  val managedScalaPath = SettingKey[File]("managed-scala-path")
  val managedJavaPath = SettingKey[File]("managed-java-path")
  val managedNativePath = SettingKey[File]("managed-native-path")

  /** Default Settings */
  val envs = SettingKey[Seq[String]]("envs")
}
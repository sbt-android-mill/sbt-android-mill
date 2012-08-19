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

package sbt.android.mill.aidl

import sbt.Keys._
import sbt._
import sbt.android.mill.MillKeys._
import sbt.android.mill.MillStage
import stopwatch.Stopwatch
import sbt.android.mill.Mill

object AIDL extends MillStage {
  lazy val stagePrepareKey = aidlStagePrepare
  lazy val stageCoreKey = aidlStageCore
  lazy val stageFinalizerKey = aidlStageFinalizer
  val DefaultAIDLName = "aidl"

  val settings: Seq[Project.Setting[_]] = Seq(
    aidlName := DefaultAIDLName,
    aidlPath <<= (platformToolsPath, aidlName)(_ / _),
    aidlStagePrepare <<= stagePrepareTask,
    aidlStagePrepare <<= aidlStagePrepare dependsOn (preStagePrepare),
    aidlStageCore <<= aidlStageCoreTask,
    aidlStageCore <<= aidlStageCore dependsOn aidlStagePrepare,
    aidlStageFinalizer <<= stageFinalizerTask,
    aidlStageFinalizer <<= aidlStageFinalizer dependsOn (aidlStagePrepare, aidlStageCore),
    sourceGenerators in Compile <+= aidlStageCore)

  def aidlStageCoreTask =
    (sourceDirectories in Compile, aidlPath, platformPath, managedJavaPath, javaSource, scalaSource in Compile, streams) map {
      (sDirs, aidlPath, platformPath, javaPath, jSource, sSource, s) =>
        task(s.log) {
          val aidlPaths = sDirs.map(_ ** "*.aidl").reduceLeft(_ +++ _).get
          if (aidlPaths.isEmpty) {
            s.log.debug(header() + "no AIDL files found, skipping")
            Nil
          } else {
            val processor = aidlPaths.map { ap =>
              aidlPath.absolutePath ::
                "-o" + javaPath.absolutePath ::
                "-I" + jSource.absolutePath ::
                "-I" + sSource.absolutePath ::
                "-p" + (platformPath / "framework.aidl").absolutePath ::
                ap.absolutePath :: Nil
            }.foldLeft(None.asInstanceOf[Option[ProcessBuilder]]) { (f, s) =>
              f match {
                case None => Some(s)
                case Some(first) => Some(first #&& s)
              }
            }.get
            s.log.debug(header() + "generating aidl " + processor)
            processor !

            val rPath = javaPath ** "R.java"
            javaPath ** "*.java" --- (rPath) get
          }
        }
    }
  def aidlStageTask =
    (streams) map {
      (s) =>
    }
}

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

package sbt.android.mill.aidl

import sbt.Keys._
import sbt._
import sbt.android.mill.AndroidKeys._
import sbt.android.mill.MillStage
import sbt.android.mill.AndroidMill

object Main extends MillStage {
  @volatile private var profiling: Option[stopwatch.Stopwatch] = None
  val before = aidlBefore
  val stage = aidlStage
  val after = aidlAfter

  val DefaultAaidlName = "aidl"

  def aidlBeforeTask =
    (sourceDirectories, aidlPath, platformPath, managedJavaPath, javaSource, streams) map {
      (sDirs, aidlPath, platformPath, javaPath, jSource, s) =>
        s.log.debug("enter aidl-before")
        profiling = Some(AndroidMill.profiling.start("aidl"))
        false
    }
  def aidlStageTask =
    (aidlBefore, sourceDirectories, aidlPath, platformPath, managedJavaPath, javaSource, streams) map {
      (aidlBefore, sDirs, aidlPath, platformPath, javaPath, jSource, s) =>
        s.log.debug("enter aidl-generate")
        val aidlPaths = sDirs.map(_ ** "*.aidl").reduceLeft(_ +++ _).get
        if (aidlPaths.isEmpty) {
          s.log.debug("no AIDL files found, skipping")
          Nil
        } else {
          val processor = aidlPaths.map { ap =>
            aidlPath.absolutePath ::
              "-p" + (platformPath / "framework.aidl").absolutePath ::
              "-o" + javaPath.absolutePath ::
              "-I" + jSource.absolutePath ::
              ap.absolutePath :: Nil
          }.foldLeft(None.asInstanceOf[Option[ProcessBuilder]]) { (f, s) =>
            f match {
              case None => Some(s)
              case Some(first) => Some(first #&& s)
            }
          }.get
          s.log.debug("generating aidl " + processor)
          processor !

          val rPath = javaPath ** "R.java"
          javaPath ** "*.java" --- (rPath) get
        }
    }
  def aidlAfterTask =
    (aidlStage, sourceDirectories, aidlPath, platformPath, managedJavaPath, javaSource, streams) map {
      (aidlStage, sDirs, aidlPath, platformPath, javaPath, jSource, s) =>
        s.log.debug("enter aidl-after")
        profiling.foreach(_.stop)
    }

  val settings = Seq(
    aidlName := DefaultAaidlName,
    aidlPath <<= (platformToolsPath, aidlName)(_ / _),
    aidlBefore <<= aidlBeforeTask,
    aidlStage <<= aidlStageTask,
    aidlAfter <<= aidlAfterTask)

  /*trait Main {
  this: AndroidFabric =>

  /*    (streams) map {
      (s) =>
        s.log.debug("aidl in thread " + Thread.currentThread.getId)
                for (i <- 0 to 10) {
          s.log.debug(Thread.currentThread.getId + " ! " + i)
          Thread.sleep(1000)
        }

    }*/
  val settingsStage3 = inConfig(fabricConf)(Seq(
    ))
}
*/
}

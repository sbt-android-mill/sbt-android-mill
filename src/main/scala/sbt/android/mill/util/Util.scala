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

package sbt.android.mill.util

object Util {
  def thread[F](f: => F) = {
    val thread = new Thread(new Runnable() { def run() { f } })
    thread.start
    thread
  }
  /*def startTask(emulator: Boolean) =
    (dbPath, manifestSchema, manifestPackage, manifestPath, streams) map {
      (dp, schema, mPackage, amPath, s) =>
      adbTask(dp.absolutePath,
              emulator, s,
              "shell", "am", "start", "-a", "android.intent.action.MAIN",
              "-n", mPackage+"/"+
              launcherActivity(schema, amPath.head, mPackage))
  }

  def launcherActivity(schema: String, amPath: File, mPackage: String) = {
    val launcher = for (
      activity <- (manifest(amPath) \\ "activity");
      action <- (activity \\ "action");
      val name = action.attribute(schema, "name").getOrElse(sys.error {
        "action name not defined"
      }).text;
      if name == "android.intent.action.MAIN"
    ) yield {
      val act = activity.attribute(schema, "name").getOrElse(sys.error("activity name not defined")).text
      if (act.contains(".")) act else mPackage + "." + act
    }
    launcher.headOption.getOrElse("")
  }*/
}

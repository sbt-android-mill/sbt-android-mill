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

import sbt.Logger
import sbt.ProcessLogger
import sbt.Level

class ReduceLogLevelWrapper(log: Logger, newLevel: Level.Value, prefix: () => String = () => "") extends ProcessLogger {
  import scala.collection.mutable.ListBuffer
  import Level.{ Info, Warn, Error, Value => LogLevel }
  private val msgs: ListBuffer[(LogLevel, String)] = new ListBuffer()

  def bufferExists(f: ((Level.Value, String)) => Boolean): Boolean = msgs.exists(f)
  def info(s: => String): Unit = synchronized { msgs += ((Info, s)) }
  def error(s: => String): Unit = synchronized { msgs += ((Error, s)) }
  def buffer[T](f: => T): T = f
  private def print(desiredLevel: LogLevel)(t: (LogLevel, String)): String = t match {
    case (Info, msg) =>
      log.info(prefix() + msg)
      msg
    case (Error, msg) =>
      log.log(desiredLevel, prefix() + msg)
      msg
  }
  def flush(exitCode: Int): Seq[String] = {
    val level = if (exitCode == 0) newLevel else Error // reduce log level Error -> newLevel if exitCode is zero
    var result = msgs map print(level)
    msgs.clear()
    result
  }
}
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

import java.io.InputStream
import java.io.OutputStream
import java.lang.ProcessBuilder

import sbt.ProcessIO

object Compat {
  val (processIOConstructor, processIOnew) = {
    val ctor = classOf[ProcessIO].getDeclaredConstructors
    assert(ctor.length == 1, "unknown sbt.ProcessIO signature")
    (ctor.head, ctor.head.getParameterTypes.length == 4)
  }
  def process(writeInput: OutputStream => Unit, processOutput: InputStream => Unit,
    processError: InputStream => Unit, inheritInput: ProcessBuilder => Boolean = ii => false) =
    if (processIOnew)
      processIOConstructor.newInstance(writeInput, processOutput, processError, inheritInput)
    else
      processIOConstructor.newInstance(writeInput, processOutput, processError)
}

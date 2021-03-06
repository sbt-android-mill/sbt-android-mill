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

object MillClassic extends Mill {
  override def go = inConfig(MillKeys.millConf)(compositeSettings ++
    pre.Pre.settings ++
    aidl.AIDL.settings ++
    aapt.AAPT.settings ++
    compile.Compile.settings ++
    proguard.Proguard.settings ++
    dx.DX.settings ++
    target.Device.settings ++
    target.Emulator.settings ++
    ndk.NDK.settings ++
    Mill.tasksSequence) // always last!!!, at least after all tasks
}

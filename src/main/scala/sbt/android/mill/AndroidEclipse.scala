package sbt.android.mill

import sbt._
import Keys._
import sbt.Plugin

object AndroidEclipse extends AndroidMill {
  val millSettings = inConfig(millConf)(aidl.Main.settings ++ commonSettings)
}
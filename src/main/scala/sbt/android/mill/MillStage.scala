package sbt.android.mill

import sbt._
import Keys._

trait MillStage {
  val before: TaskKey[_]
  val stage: TaskKey[_]
  val after: TaskKey[_]
  val settings: Seq[Project.Setting[_]]
}
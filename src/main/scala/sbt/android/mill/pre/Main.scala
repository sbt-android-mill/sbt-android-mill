package sbt.android.mill.pre

import sbt._

import Keys._

trait Main {
  val stage1_prepare = TaskKey[Unit]("stage1_prepare")
  def stage1_prepareTask =
    (streams) map {
      (s) =>
        s.log.debug("prepare in thread " + Thread.currentThread.getId)
        for (i <- 0 to 10) {
          s.log.debug(Thread.currentThread.getId + " ! " + i)
          Thread.sleep(1000)
        }
    }
}
package sbt.android.mill.compile

import sbt._

import Keys._

trait Main {
  val stage4_compile = TaskKey[Unit]("stage4_compile")
  def stage4_compileTask =
    (streams) map {
      (s) =>
        s.log.debug("compile in thread " + Thread.currentThread.getId)
        for (i <- 0 to 10) {
          s.log.debug(Thread.currentThread.getId + " ! " + i)
          Thread.sleep(1000)
        }

    }
}

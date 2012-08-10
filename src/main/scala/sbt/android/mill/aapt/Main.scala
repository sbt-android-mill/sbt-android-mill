package sbt.android.mill.aapt

import sbt._
import Keys._

/*object Main extends StageTrait {
  val stage2_AAPT = TaskKey[Unit]("stage2_AAPT")
  def stage2_AAPTTask =
    (streams) map {
      (s) =>
        s.log.debug("aapt in thread " + Thread.currentThread.getId)
                for (i <- 0 to 10) {
          s.log.debug(Thread.currentThread.getId + " ! " + i)
          Thread.sleep(1000)
        }

    }
}
*/
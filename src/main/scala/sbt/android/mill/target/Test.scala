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

package sbt.android.mill.target

import com.android.ddmlib.testrunner.InstrumentationResultParser
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.ITestRunListener.TestFailure

import sbt._
import sbt.Keys._
import sbt.android.mill.MillKeys._
import sbt.complete.DefaultParsers._
import sbt.complete.Parser

trait Test {
  this: Target =>
  def instrumentationTestAction(emulator: Boolean, taskTag: Scoped) =
    (manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) map {
      (manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) =>
        val taskTagLabel = taskTag.key.label
        task(streams.log, taskTagLabel) {
          runTests(taskTagLabel, emulator, manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams)
        }
    }
  def runTestOnly(emulator: Boolean, taskTag: Scoped) = (test: TaskKey[String]) =>
    (test, manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) map {
      (test, manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams) =>
        val taskTagLabel = taskTag.key.label
        task(streams.log, taskTagLabel) {
          runTests(taskTagLabel, emulator, manifestPackage, targetADBPath, targetADBTimeout, targetADBLogLevel, streams,
            "-e", "class", test)
        }
    }
  def runTests(taskTagLabel: String, emulator: Boolean, manifestPackage: String, targetADBPath: File, targetADBTimeout: Int,
    targetADBLogLevel: Level.Value, streams: TaskStreams, args: String*) {
    val testRunner = manifestPackage + "/android.test.InstrumentationTestRunner"
    val action = Seq("shell", "am", "instrument", "-r", "-w") ++ args :+ testRunner
    streams.log.info(header(taskTagLabel) + "test " + testRunner)
    if (connection(targetADBTimeout))
      Target.command(emulator, targetADBPath.getAbsolutePath(), () => header(taskTagLabel), targetADBLogLevel, streams.log, action: _*) match {
        case (true, out) =>
          parseTests(out.toArray, manifestPackage, streams.log)
        case (false, out) =>
          streams.log.error("am instrument returned error\n\n" + out)
      }
    else
      streams.log.error(header(taskTagLabel) + "unable to test " + testRunner + ", " + target + " not found")
  }
  def parseTests(out: Array[String], name: String, log: Logger) {
    val listener = new Test.Listener(log)
    val parser = new InstrumentationResultParser(name, listener)
    parser.processNewLines(out.map(_.trim))
    listener.errorMessage.map(sys.error(_)).orElse {
      log.success("All tests passed")
      None
    }
  }
  def testParser(s: State, tests: Seq[String]): Parser[String] =
    Space ~> tests.map(t => token(t))
      .reduceLeftOption(_ | _)
      .getOrElse(token(NotSpace))
}

object Test {
  class Listener(log: Logger) extends ITestRunListener {
    import com.android.ddmlib.testrunner.TestIdentifier
    type Metrics = java.util.Map[String, String]

    val failedTests = new collection.mutable.ListBuffer[TestIdentifier]
    var runFailed: Option[String] = None

    def testRunStarted(runName: String, testCount: Int) =
      log.info("testing %s (%d test%s)".format(runName, testCount, if (testCount == 1) "" else "s"))
    def testRunStopped(elapsedTime: Long) =
      log.info("testRunStopped (%d seconds)".format(elapsedTime))
    def testStarted(test: TestIdentifier) =
      log.debug("testStarted: " + test)
    def testEnded(test: TestIdentifier, metrics: Metrics) {
      if (!failedTests.contains(test)) {
        val status = "%spassed%s: %s".format(scala.Console.GREEN, scala.Console.RESET, test)
        if (test.getTestName != "testAndroidTestCaseSetupProperly") {
          log.info(status)
        } else {
          log.debug(status)
        }
      }
      log.debug(metrics.toString)
    }
    def testFailed(status: TestFailure, test: TestIdentifier, trace: String) {
      log.error("failed: %s\n\n%s\n".format(test, trace))
      failedTests += test
    }
    def testRunEnded(elapsedTime: Long, metrics: Metrics) {
      log.info("testRunEnded (%d seconds)".format(elapsedTime))
      log.debug(metrics.toString)
    }
    def testRunFailed(message: String) {
      log.error("testRunFailed: " + message)
      runFailed = Some(message)
    }
    def errorMessage: Option[String] = {
      if (!failedTests.isEmpty)
        Some("Failed tests: " + failedTests.mkString(", "))
      else runFailed
    }
  }
}
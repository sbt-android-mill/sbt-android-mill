package sbt.android.mill.helloworld

import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit

import com.jayway.android.robotium.solo.Solo

import android.test.ActivityInstrumentationTestCase2

class HelloWorldActivityTest
  extends ActivityInstrumentationTestCase2[HelloWorld](classOf[HelloWorld])
  with JUnitSuite with ShouldMatchersForJUnit {
  @volatile private var solo: Solo = null
  @volatile private var activity: HelloWorld = null

  def testHelloWorld() {
    android.util.Log.i("HelloWorldTest", "testHelloWorld BEGIN")

    true should be (true)

    activity should not be (null)

    android.util.Log.i("HelloWorldTest", "testHelloWorld END")
  }

  override def setUp() {
    super.setUp()
    activity = getActivity
    solo = new Solo(getInstrumentation(), activity)
  }
  override def tearDown() = {
    try {
      solo.finalize()
    } catch {
      case e =>
        e.printStackTrace()
    }
    activity.finish()
    super.tearDown()
  }
}

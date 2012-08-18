run tests from command line: adb shell am instrument -w sbt.android.mill.helloworld/android.test.InstrumentationTestRunner

list tests: show defined-test-names

sbt> show defined-test-names

sbt> android-mill:emulator-install

sbt> android-mill:emulator-test

sbt> android-mill:emulator-test-only sbt.android.mill.helloworld.HelloWorldActivityTest

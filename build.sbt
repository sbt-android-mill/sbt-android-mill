sbtPlugin := true

name := "sbt-android-mill"

organization := "sbt.android.mill"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

resolvers += "stopwatch" at "http://sbt-android-mill.github.com/stopwatch/releases"

libraryDependencies ++= Seq(
    "org.digimead" %% "stopwatch-core" % "1.0-SNAPSHOT",
    "com.google.android.tools" % "ddmlib" % "r10",
    "net.sf.proguard" % "proguard-base" % "4.8"
)

import sbt.android.mill.MillKeys._
import Types._
import Path._

sbt.android.mill.MillClassic.projectSettings

name := "Simple"

version := "0.1"

scalaVersion := "2.8.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug

platformName in millConf := "android-16"

mainResPath in millConf <<= (baseDirectory, resDirectoryName in millConf) map (_ / ".." / "HelloWorld" / "src" / "main" / _)

unmanagedSourceDirectories in Compile <+= (baseDirectory) (_ / "src" / "test" / "scala") // add tests to package

unmanagedSourceDirectories in Compile <+= (baseDirectory) (_ / ".." / "HelloWorld" / "src" / "main" / "scala") // point to original sources

libraryDependencies ++= {
  Seq(
    "org.scalatest" %% "scalatest" % "1.8",
    "com.jayway.android.robotium" % "robotium-solo" % "3.2.1"
  )
}

import sbt.android.mill.MillKeys._

sbt.android.mill.MillEclipse.go

name := "HelloWorldEclipse"

version := "0.1"

scalaVersion := "2.8.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug

platformName in millConf := "android-16"

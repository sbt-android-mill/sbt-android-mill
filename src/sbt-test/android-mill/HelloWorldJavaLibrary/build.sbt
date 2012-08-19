import sbt._
import sbt.android.mill.MillKeys._

sbt.android.mill.MillClassic.go

name := "Simple"

version := "0.1"

scalaVersion := "2.8.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug

platformName in millConf := "android-16"

libraries in millConf <<= (baseDirectory) map { base => Seq(LibraryProject("sbt.android.mill.androidlib", base / "android-library" / "AndroidManifest.xml",
  Set[File](), Some(base / "android-library" / "res"), None)) }

unmanagedBase <<= (baseDirectory) (_ / "android-library" / "bin")
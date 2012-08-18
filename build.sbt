import com.jsuereth.sbtsite.SiteKeys

name := "sbt-android-mill"

organization := "sbt.android.mill"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

libraryDependencies ++= Seq(
    "org.digimead" %% "stopwatch-core" % "1.0-SNAPSHOT",
    "com.google.android.tools" % "ddmlib" % "r10",
    "net.sf.proguard" % "proguard-base" % "4.8"
)

sbtPlugin := true

ScriptedPlugin.scriptedSettings

site.settings

//ghpages.settings

//git.remoteRepo := "git@github.com:{your username}/{your project}.git"

//logLevel := Level.Debug

sbt.source.align.Align.alignSettings

resolvers += "stopwatch" at "http://sbt-android-mill.github.com/stopwatch/releases"

scriptedLaunchOpts ++= {
  import scala.collection.JavaConverters._
  val args = Seq("-Xmx8196M","-Xms8196M")
  management.ManagementFactory.getRuntimeMXBean().getInputArguments().asScala.filter(a => args.contains(a) || a.startsWith("-XX")).toSeq
}

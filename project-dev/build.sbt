import com.jsuereth.sbtsite.SiteKeys

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


ScriptedPlugin.scriptedSettings

sbt.source.align.Align.alignSettings

site.settings

scriptedLaunchOpts ++= {
  import scala.collection.JavaConverters._
  val args = Seq("-Xmx8196M","-Xms8196M")
  management.ManagementFactory.getRuntimeMXBean().getInputArguments().asScala.filter(a => args.contains(a) || a.startsWith("-XX")).toSeq
}

sourceDirectory <<= (baseDirectory) (_ / ".." / "src")

target <<= (baseDirectory) (_ / ".." / "target")
import com.jsuereth.sbtsite.SiteKeys

name := "sbt-android-mill"

organization := "sbt.android.mill"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

publishMavenStyle := false

publishTo <<= (version) { version: String =>
    val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
    val (name, url) = if (version.contains("-"))
                        ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                      else
                        ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
    Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "com.google.android.tools" % "ddmlib" % "r10",
  "net.sf.proguard" % "proguard-base" % "4.6"
)

sbtPlugin := true

commands += Status.stampVersion

ScriptedPlugin.scriptedSettings

seq(site.settings:_*)

seq(ghpages.settings:_*)

git.remoteRepo := "git@github.com:{your username}/{your project}.git"

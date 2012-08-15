resolvers += Classpaths.typesafeResolver

addSbtPlugin("sbt.android.mill" % "sbt-android-mill" % "0.1-SNAPSHOT")

resolvers += "stopwatch" at "http://sbt-android-mill.github.com/stopwatch/releases"

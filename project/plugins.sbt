resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)

addSbtPlugin("com.jsuereth" % "sbt-ghpages-plugin" % "0.4.0")

libraryDependencies <+= sbtVersion(v=>
  v.split('.') match {
    case Array("0", "11", "2") =>
      "org.scala-tools.sbt" %% "scripted-plugin" % v
    case Array("0", "11", "3") =>
      "org.scala-sbt" %% "scripted-plugin" % v
    case _ =>
      "org.scala-sbt" % "scripted-plugin" % v
  }
)

resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)

resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.jsuereth" % "sbt-ghpages-plugin" % "0.4.0")

libraryDependencies <+= (sbtVersion) { (sv) => try {
    // notify before project loaded
    if (sv.split("""\.""")(1).toInt <= 11) {
      System.out.print("loading < 0.12 scripted-plugin\n")
      "org.scala-sbt" %% "scripted-plugin" % sv
    } else {
      System.out.print("loading >=0.12 scripted-plugin\n")
      "org.scala-sbt" % "scripted-plugin" % sv
    }
  } catch {
    case e =>
      System.out.print("loading >=0.12 scripted-plugin - " + e.getMessage + "\n")
      "org.scala-sbt" % "scripted-plugin" % sv // unable to parse - something new, so assume that we have new version
  }
}

resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)

resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" %% "scripted-plugin" % sv
}

addSbtPlugin("com.jsuereth" % "sbt-ghpages-plugin" % "0.4.0")

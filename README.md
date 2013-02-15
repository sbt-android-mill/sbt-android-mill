sbt-android-mill
================

sbt-android-mill - simple-build-tool multi-thread plugin with profiling

If you want improve it, please send mail to sbt-android-mill at digimead.org. You will be added to the group. Please, feel free to add yourself to authors.

There are base HelloWorld projects:

* [HelloWorld](https://github.com/sbt-android-mill/sbt-android-mill/tree/master/src/sbt-test/android-mill/HelloWorld)
* [HelloWorldEclipse](https://github.com/sbt-android-mill/sbt-android-mill/tree/master/src/sbt-test/android-mill/HelloWorldEclipse)
* [HelloWorldInstrumentation](https://github.com/sbt-android-mill/sbt-android-mill/tree/master/src/sbt-test/android-mill/HelloWorldInstrumentation)
* [HelloWorldJavaLibrary](https://github.com/sbt-android-mill/sbt-android-mill/tree/master/src/sbt-test/android-mill/HelloWorldJavaLibrary)
* [HelloWorldJNI](https://github.com/sbt-android-mill/sbt-android-mill/tree/master/src/sbt-test/android-mill/HelloWorldJNI)

Please, read [sbt.android.mill.MillKeys](https://github.com/sbt-android-mill/sbt-android-mill/blob/master/src/main/scala/sbt/android/mill/MillKeys.scala). It is very very easy to read.

## Environment ##

Please note that Android SDK with Scala is best suited for Oracle Java 1.6.

If you want to use Java 1.7 please turn off optimization.

```scala    
scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-Xcheckinit") ++
  (if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")) // -optimize fails with jdk7
```

Using OpenJDK lead to incorrect bytecode interpretation in Android dx tool.

Example development environment is:
* Eclipse 3.7.1 with Scala 2.9 Nighly and latest ADK based on **Java 7**
* SBT 0.12.x with Scala 2.8.2 based on **Java 6**

So we develop our applications in Scala 2.9.x environment and compile it with Scala 2.8.x

## Participate in the development ##

Branches:

* origin/master reflects a production-ready state
* origin/release-* support preparation of a new production release. Allow for last-minute dotting of i’s and crossing t’s
* origin/hotfix-* support preparation of a new unplanned production release
* origin/develop reflects a state with the latest delivered development changes for the next release (nightly builds)
* origin/feature-* new features for the upcoming or a distant future release

Structure of branches follow strategy of http://nvie.com/posts/a-successful-git-branching-model/

If you will create new origin/feature-* please open feature request for yourself.

* Anyone may comment you feature here.
* We will have a history for feature and ground for documentation
* If week passed and there wasn't any activity + all tests passed = release a new version ;-)

## Adding to your project ##

Create a

 * _project/plugins/project/Build.scala_ - for older simple-build-tool
 * _project/project/Build.scala_ - for newer simple-build-tool

file that looks like the following:

```scala
    import sbt._
    object PluginDef extends Build {
      override def projects = Seq(root)
      lazy val root = Project("plugins", file(".")) dependsOn(mill)
      lazy val mill = uri("git://github.com/sbt-android-mill/sbt-android-mill.git#0.2")
    }
```

You may find more information about Build.scala at [https://github.com/harrah/xsbt/wiki/Plugins](https://github.com/harrah/xsbt/wiki/Plugins)

Then in your _build.sbt_ file, simply add:

``` scala
    sbt.android.mill.MillClassic.go
```

or

``` scala
    sbt.android.mill.MillEclipse.go
```

## packages - build stages relation

![packages structure](https://github.com/sbt-android-mill/sbt-android-mill/blob/master/notes/build-sbt-android-mill.png?raw=true)

## workflow / structure

![packages structure](https://github.com/sbt-android-mill/sbt-android-mill/blob/master/notes/sbt-android-mill.png?raw=true)

FAQ
---

q. I don't like artifact name with ```package-bin```. How I may change it?

a. You may setup your artifact name explicit:

```scala
artifactName := ((version: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + module.revision + "." + artifact.extension )
  ```

AUTHORS
-------

* Alexey Aksenov


all previous authors was dropped, because sbt-android-plugin licence is contains

_3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission._

LICENSE
-------

The sbt-android-mill is licensed to you under the terms of
the Apache License, version 2.0, a copy of which has been
included in the LICENSE file.

Copyright
---------

Copyright © 2012 Alexey B. Aksenov/Ezh. All rights reserved.

This plugin based on sbt-android-plugin (c) 2009 Walter Chang, Mark Harrah, Jan Berkel

sbt-android-plugin licence located in LICENSE.sbt-android-plugin

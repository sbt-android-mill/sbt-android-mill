/**
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.android.mill.proguard

import java.io.{ File => JFile }
import java.util.Properties

import proguard.{ Configuration => ProGuardConfiguration, ProGuard, ConfigurationParser }

import sbt._
import sbt.Keys._
import sbt.android.mill.MillStage
import sbt.android.mill.MillKeys._
import sbt.Project

object Proguard extends MillStage {
  lazy val stagePrepareKey = proguardStagePrepare
  lazy val stageCoreKey = proguardStageCore
  lazy val stageFinalizerKey = proguardStageFinalizer
  val DefaultProjectJarName = "classes.min.jar"

  lazy val settings: Seq[Project.Setting[_]] = Seq(
    proguardEnabled := true,
    proguardProjectJarName := DefaultProjectJarName,
    proguardProjectJarPath <<= (target, proguardProjectJarName)(_ / _),
    proguardOptimizations := Seq.empty,
    proguardOption <<= proguardOptionTask,
    proguardExclude <<= proguardExcludeTask,
    proguardInJars <<= proguardInJarsTask,
    proguardStagePrepare <<= stagePrepareTask,
    proguardStagePrepare <<= proguardStagePrepare dependsOn (preStagePrepare),
    proguardStageCore <<= proguardStageCoreTask,
    proguardStageCore <<= proguardStageCore dependsOn proguardStagePrepare,
    proguardStageFinalizer <<= stageFinalizerTask,
    proguardStageFinalizer <<= proguardStageFinalizer dependsOn (proguardStagePrepare, proguardStageCore))

  def proguardStageCoreTask: Project.Initialize[Task[Option[File]]] =
    (proguardEnabled, proguardOptimizations, classDirectory, proguardInJars, streams,
      proguardProjectJarPath, jarPathSDK, proguardOption) map {
        (proguardEnabled, proguardOptimizations, classDirectory, proguardInJars, streams,
        classesMinJarPath, jarPathSDK, proguardOption) =>
          task(streams.log) {
            if (proguardEnabled) {
              val optimizationOptions = if (proguardOptimizations.isEmpty) Seq("-dontoptimize") else proguardOptimizations
              val manifestr = List("!META-INF/MANIFEST.MF", "R.class", "R$*.class",
                "TR.class", "TR$.class", "library.properties")
              val sep = JFile.pathSeparator
              val inJars = ("\"" + classDirectory.absolutePath + "\"") +:
                proguardInJars.map("\"" + _ + "\"" + manifestr.mkString("(", ",!**/", ")"))

              val args = (
                "-injars" :: inJars.mkString(sep) ::
                "-outjars" :: "\"" + classesMinJarPath.absolutePath + "\"" ::
                "-libraryjars" :: jarPathSDK.get.map("\"" + _ + "\"").mkString(sep) ::
                Nil) ++ optimizationOptions ++ proguardOption
              streams.log.debug("executing proguard: " + (for (i <- 0 until args.size) yield { "arg" + (i + 1) + ": " + args(i) }).mkString("\n"))
              val config = new ProGuardConfiguration
              new ConfigurationParser(args.toArray[String], new Properties).parse(config)
              streams.log.debug("executing proguard: " + args.mkString("\n"))
              new ProGuard(config).execute
              Some(classesMinJarPath)
            } else {
              streams.log.info("Skipping Proguard")
              None
            }
          }
      }
  def proguardExcludeTask = (jarPathSDK, classDirectory, resourceDirectory) map {
    (libPath, classDirectory, resourceDirectory) =>
      libPath.get :+ classDirectory :+ resourceDirectory
  }
  def proguardInJarsTask = (fullClasspath, proguardExclude, preinstalledModules, classpathTypes in Compile) map {
    (fullClasspath, proguardExclude, preinstalledModules, classpathTypes) =>
      // remove preinstalled jars
      fullClasspath.filterNot(cp =>
        cp.get(moduleID.key).map(module => preinstalledModules.exists(m =>
          m.organization == module.organization &&
            m.name == module.name)).getOrElse(false) // only include jar files
            ).filter(cp =>
        cp.get(artifact.key).map(artifact => (classpathTypes - "so").contains(artifact.`type`)).getOrElse(true)).map(_.data) --- proguardExclude get
  }
  def proguardOptionTask: Project.Initialize[Task[Seq[String]]] = (manifestPackage) map { (manifestPackage) =>
    ("-dontwarn" :: "-dontobfuscate" ::
      "-dontnote scala.Enumeration" ::
      "-dontnote org.xml.sax.EntityResolver" ::
      "-keep public class * extends android.app.Activity" ::
      "-keep public class * extends android.app.Service" ::
      "-keep public class * extends android.app.backup.BackupAgent" ::
      "-keep public class * extends android.appwidget.AppWidgetProvider" ::
      "-keep public class * extends android.content.BroadcastReceiver" ::
      "-keep public class * extends android.content.ContentProvider" ::
      "-keep public class * extends android.view.View" ::
      "-keep public class * extends android.app.Application" ::
      "-keep public class " + manifestPackage + ".** { public protected *; }" ::
      "-keep public class * implements junit.framework.Test { public void test*(); }" ::
      """
                  -keepclassmembers class * implements java.io.Serializable {
                    private static final java.io.ObjectStreamField[] serialPersistentFields;
                    private void writeObject(java.io.ObjectOutputStream);
                    private void readObject(java.io.ObjectInputStream);
                    java.lang.Object writeReplace();
                    java.lang.Object readResolve();
                   }
                   """ :: Nil)
  }
}

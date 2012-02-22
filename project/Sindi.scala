import sbt._
import Keys._
import ProguardPlugin._
import fmpp.FmppPlugin._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Sonatype.settings ++ Seq(
    organization        := "com.github.aloiscochard.sindi",
    version             := "0.5",
    scalaVersion        := "2.9.1",
    scalacOptions       := Seq("-unchecked", "-deprecation"),
    crossScalaVersions  := Seq("2.9.0-1", "2.9.1")
  )
}

object Resolvers {
  val sonatypeOssReleases = "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
  val sontaypeOssSnapshots = "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
  val scalaToolsReleases = "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}

object Dependencies {
  val testDependencies = Seq(libraryDependencies <<= (scalaVersion, libraryDependencies) { (version, dependencies) =>
    val specs2 = version match {
      case "2.9.1" => ("org.specs2" %% "specs2" % "1.6.1" % "test")
      case _ => ("org.specs2" %% "specs2" % "1.5" % "test")
    }
    dependencies :+ specs2
  })
}


object SindiBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val sindi = Project (
    "sindi",
    file ("."),
    settings = buildSettings
  ) aggregate (core, compiler)

  // SINDI-CORE //
  lazy val core = Project(
    "sindi-core",
    file("sindi-core"),
    settings = buildSettings ++ testDependencies ++ fmppSettings ++ Seq(
      // WORKAROUND for https://github.com/harrah/xsbt/issues/85
      unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
    )
  ) configs (Fmpp)

  // SINDI-COMPILER //
  val assembly = TaskKey[Unit]("assembly")

  lazy val compiler = Project(
    "sindi-compiler",
    file("sindi-compiler"),
    settings = buildSettings ++ testDependencies ++ Seq(
                  libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ % "provided")
                ) ++ seq(
                  (proguardSettings ++ seq(
                    proguardInJars := Seq(),
                    proguardLibraryJars <++=
                      (update) map (_.select(module =
                        moduleFilter(name = "scala-compiler") | moduleFilter(name = "scala-library"))),
                    proguardOptions ++= Seq("-keep class sindi.** { *; }")
                  ):_*) 
                ) ++ Seq(
                  assembly <<= (clean, proguard, minJarPath, artifactPath in (Compile, packageBin), streams) map {
                    (_, _, min, artifact, streams) => {
                      streams.log("assembly").info("copy %s --> %s".format(min, artifact))
                      IO.copyFile(min, artifact)
                    }
                  },
                  publish <<= publish.dependsOn(assembly),
                  publishLocal <<= publishLocal.dependsOn(assembly)
                )
  ) dependsOn (core)
}

object Sonatype extends PublishToSonatype(SindiBuild) {
  def projectUrl    = "https://github.com/aloiscochard/sindi"
  def developerId   = "alois.cochard"
  def developerName = "Alois Cochard"
  def licenseName   = "Apache 2 License"
  def licenseUrl    = "http://www.apache.org/licenses/LICENSE-2.0.html"
}

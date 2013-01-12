import sbt._
import Keys._

import fmpp.FmppPlugin._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Sonatype.settings ++ Seq(
    organization        := "com.github.aloiscochard.sindi",
    version             := "1.0-SNAPSHOT",
    scalaVersion        := "2.10.0",
    scalacOptions       <++= scalaVersion.map(_ match {
      case "2.10.0" => Seq("-unchecked", "-deprecation")
      case _ => Seq("-unchecked", "-deprecation", "-Ydependent-method-types")
    }),
    crossScalaVersions  := Seq("2.9.1-1", "2.9.2", "2.10.0")
  )
}

object Resolvers {
  val sonatypeOssReleases = "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
  val sontaypeOssSnapshots = "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
}

object Dependencies {
  val testDependencies = Seq(
    libraryDependencies <+= scalaVersion(_ match {
      case "2.10.0" => "org.specs2" %% "specs2" % "1.13" % "test"
      case _ => "org.specs2" %% "specs2" % "1.11" % "test"
    })
  )
}


object SindiBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val sindi = Project (
    "sindi",
    file ("."),
    settings = buildSettings ++ Seq(publishArtifact := false)
  ) aggregate (core, config, examples_app)

  lazy val core = Project(
    "sindi-core",
    file("sindi-core"),
    settings = buildSettings ++ testDependencies ++ fmppSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
      unmanagedSourceDirectories in Compile <+= (sourceDirectory in Compile, scalaVersion) { (sourceDirectory, scalaVersion) =>
        scalaVersion match {
          case v if v.startsWith("2.10") => sourceDirectory / "scala-2.10"
          case _ => sourceDirectory / "scala-2.9"
        }
      }
    )
  ) configs(Fmpp) dependsOn(config % "provided")

  lazy val config = Project(
    "sindi-config",
    file("sindi-config"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies += "com.typesafe" % "config" % "1.0.0"
    ) 
  )

  lazy val examples_app = Project(
    "sindi-examples_app",
    file("sindi-examples/app"),
    settings = buildSettings ++ testDependencies ++ Seq(publishArtifact := false)
  ) dependsOn(core, config)
}

object Sonatype extends PublishToSonatype(SindiBuild) {
  def projectUrl    = "https://github.com/aloiscochard/sindi"
  def developerId   = "alois.cochard"
  def developerName = "Alois Cochard"
  def licenseName   = "Apache 2 License"
  def licenseUrl    = "http://www.apache.org/licenses/LICENSE-2.0.html"
}

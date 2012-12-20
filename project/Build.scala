import sbt._
import Keys._

import fmpp.FmppPlugin._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Sonatype.settings ++ Seq(
    organization        := "com.github.aloiscochard.sindi",
    version             := "1.0-RC2",
    scalaVersion        := "2.9.2",
    scalacOptions       := Seq("-unchecked", "-deprecation", "-Ydependent-method-types"),
    crossScalaVersions  := Seq("2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2")
  )
}

object Resolvers {
  val sonatypeOssReleases = "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
  val sontaypeOssSnapshots = "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
}

object Dependencies {
  val testDependencies = Seq(
    libraryDependencies += "org.specs2" %% "specs2" % "1.11" % "test"
  )
}


object SindiBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val sindi = Project (
    "sindi",
    file ("."),
    settings = buildSettings
  ) aggregate (core, config)

  lazy val core = Project(
    "sindi-core",
    file("sindi-core"),
    settings = buildSettings ++ testDependencies ++ fmppSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
      // WORKAROUND for https://github.com/harrah/xsbt/issues/85
      unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
    )
  ) configs(Fmpp) dependsOn(config % "provided")

  lazy val config = Project(
    "sindi-config",
    file("sindi-config"),
    settings = buildSettings ++ testDependencies ++ Seq(
      libraryDependencies += "com.typesafe" % "config" % "0.5.0"
    ) 
  )
}

object Sonatype extends PublishToSonatype(SindiBuild) {
  def projectUrl    = "https://github.com/aloiscochard/sindi"
  def developerId   = "alois.cochard"
  def developerName = "Alois Cochard"
  def licenseName   = "Apache 2 License"
  def licenseUrl    = "http://www.apache.org/licenses/LICENSE-2.0.html"
}

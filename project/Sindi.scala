import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Sonatype.settings ++ Seq(
    organization        := "com.github.aloiscochard.sindi",
    version             := "1.0-SNAPSHOT",
    scalaVersion        := "2.10.0-M4",
    scalacOptions       := Seq("-unchecked", "-deprecation")
    //crossScalaVersions  := Seq("2.10.0")
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
  ) aggregate (core)

  // SINDI-CORE //
  lazy val core = Project(
    "sindi-core",
    file("sindi-core"),
    settings = buildSettings ++ testDependencies ++ Seq(
      // WORKAROUND for https://github.com/harrah/xsbt/issues/85
      unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
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

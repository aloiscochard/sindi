import sbt._
import Keys._
import ProguardPlugin._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization        := "com.github.aloiscochard.sindi",
    version             := "0.4-SNAPSHOT",
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
    settings = buildSettings ++
                Seq(publishArtifact in (Compile, packageBin) := false) ++
                Seq(publishArtifact in (Compile, packageDoc) := false) ++
                Seq(publishArtifact in (Compile, packageSrc) := false) 
  ) aggregate (core, compiler)

  lazy val core = Project(
    "sindi-core",
    file("sindi-core"),
    settings = buildSettings ++ Sonatype.settings ++ testDependencies ++
                // WORKAROUND for https://github.com/harrah/xsbt/issues/85
                // Remove when updated to SBT 0.11
                Seq(unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")))
  ) 

  lazy val compiler = Project(
    "sindi-compiler",
    file("sindi-compiler"),
    settings = buildSettings ++ Sonatype.settings ++ testDependencies ++
                  Seq(
                    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ % "provided")
                  ) ++
                  seq(
                    (proguardSettings ++ seq(
                      proguardInJars := Seq(),
                      proguardLibraryJars <++=
                        (update) map (_.select(module =
                          moduleFilter(name = "scala-compiler") | moduleFilter(name = "scala-library"))),
                      proguardOptions ++= Seq("-keep class sindi.** { *; }")
                    ):_*)
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

/***********************
 * Sonatype Publishing *
 ***********************/

abstract class PublishToSonatype(build: Build) {
  import build._

  val ossSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  val ossStaging   = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
  
  def projectUrl: String
  def developerId: String
  def developerName: String
  
  def licenseName: String
  def licenseUrl: String
  def licenseDistribution = "repo"
  def scmUrl              = projectUrl
  def scmConnection       = "scm:git:" + scmUrl

  def generatePomExtra(scalaVersion: String): xml.NodeSeq = {
    <url>{ projectUrl }</url>
      <licenses>
        <license>
          <name>{ licenseName }</name>
          <url>{ licenseUrl }</url>
          <distribution>{ licenseDistribution }</distribution>
        </license>
      </licenses>
    <scm>
      <url>{ scmUrl }</url>
      <connection>{ scmConnection }</connection>
    </scm>
    <developers>
      <developer>
        <id>{ developerId }</id>
        <name>{ developerName }</name>
      </developer>
    </developers>
  }

  def settings: Seq[Setting[_]] = Seq(
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishTo <<= version((v: String) => Some( if (v.trim endsWith "SNAPSHOT") ossSnapshots else ossStaging)),
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    pomExtra <<= (scalaVersion)(generatePomExtra)
  )
}

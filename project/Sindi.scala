import sbt._
import Keys._
import ProguardPlugin._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization        := "org.scala-tools.sindi",
    version             := "0.3-SNAPSHOT",
    scalaVersion        := "2.9.0-1",
    scalacOptions       := Seq("-unchecked", "-deprecation"),
    crossScalaVersions  := Seq("2.9.0-1", "2.9.1.RC3")
  )

  val publishSettings = Seq(
    publishTo <<= (version) { version: String =>
      val nexus = "http://nexus.scala-tools.org/content/repositories/"
      if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
      else                                   Some("releases"  at nexus + "releases/")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )
}

object Resolvers {
  val scalaToolsReleases = "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}

object Dependencies {
  val testDependencies = Seq(libraryDependencies <<= (scalaVersion, libraryDependencies) { (version, dependencies) =>
    val specs2Version = version match {
      case "2.9.1.RC3" => "1.6-SNAPSHOT"
      case _ => "1.5"
    }
    dependencies :+ ("org.specs2" %% "specs2" % specs2Version % "test")
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
    settings = buildSettings ++ publishSettings ++ testDependencies ++
                // WORKAROUND for https://github.com/harrah/xsbt/issues/85
                // Remove when updated to SBT 0.11
                Seq(unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")))
  ) 

  lazy val compiler = Project(
    "sindi-compiler",
    file("sindi-compiler"),
    settings = buildSettings ++ publishSettings ++ testDependencies ++
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

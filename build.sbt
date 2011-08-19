name := "sindi"

version := "0.3-SNAPSHOT"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.0-1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

crossScalaVersions := Seq("2.9.0-1", "2.9.1.RC3")


libraryDependencies <<= (scalaVersion, libraryDependencies) { (version, dependencies) =>
  val specs2Version = version match {
    case "2.9.1.RC3" => "1.6-SNAPSHOT"
    case _ => "1.5"
  }
  dependencies :+ ("org.specs2" %% "specs2" % specs2Version % "test")
}

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ % "provided")


resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"


publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

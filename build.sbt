name := "sindi"

version := "0.2-SNAPSHOT"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.0-1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

crossScalaVersions := Seq("2.8.1", "2.9.0-1", "2.9.1.RC1")


libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.5" % "test"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ % "provided")


resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"


publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

name := "demo"

version := "0.3-SNAPSHOT"

organization := "org.scala-tools.sindi.examples"

scalaVersion := "2.9.0-1"

scalacOptions += "-unchecked"

crossScalaVersions := Seq("2.9.0-1", "2.9.1.RC3")


libraryDependencies += "org.scala-tools.sindi" %% "sindi" % "0.3-SNAPSHOT"


resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"


autoCompilerPlugins := true

//addCompilerPlugin("org.scala-tools.sindi" %% "sindi" % "0.3-SNAPSHOT")

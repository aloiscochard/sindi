name := "examples.basic"

version := "0.1"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.0-1"

crossScalaVersions := Seq("2.8.1", "2.9.0-1")


libraryDependencies += "org.scala-tools.sindi" %% "sindi" % "0.1-SNAPSHOT"


resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"


autoCompilerPlugins := true

addCompilerPlugin("org.scala-tools.sindi" %% "sindi" % "0.1-SNAPSHOT")

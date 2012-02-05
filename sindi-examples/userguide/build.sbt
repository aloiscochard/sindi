name := "sindi-examples-userguide"

version := "0.4-SNAPSHOT"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.1"

scalacOptions += "-unchecked"

scalacOptions += "-P:sindi:verbose"

crossScalaVersions := Seq("2.9.0-1", "2.9.1")

autoCompilerPlugins := true

libraryDependencies <++= version { version => Seq(
  "org.scala-tools.sindi" %% "sindi-core" % version,
  compilerPlugin("org.scala-tools.sindi" %% "sindi-compiler" % version)
)}

resolvers ++= Seq(
  "Scala-Tools Maven2 Releases" at "http://scala-tools.org/repo-releases",
  "Scala-Tools Maven2 Snapshots" at "http://scala-tools.org/repo-snapshots"
)

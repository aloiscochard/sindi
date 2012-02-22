name := "sindi-examples-userguide"

version := "0.5"

organization := "com.github.aloiscochard.sindi"

scalaVersion := "2.9.1"

scalacOptions += "-unchecked"

scalacOptions += "-P:sindi:verbose"

crossScalaVersions := Seq("2.9.0-1", "2.9.1")

autoCompilerPlugins := true

libraryDependencies <++= version { version => Seq(
  "com.github.aloiscochard.sindi" %% "sindi-core" % version,
  compilerPlugin("com.github.aloiscochard.sindi" %% "sindi-compiler" % version)
)}

resolvers ++= Seq(
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

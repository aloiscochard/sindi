name := "sindi-examples-bowler"

version := "0.4-SNAPSHOT"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.1"

scalacOptions += "-unchecked"

scalacOptions += "-P:sindi:verbose"

crossScalaVersions := Seq("2.9.0-1", "2.9.1")

autoCompilerPlugins := true

libraryDependencies <++= version { version => Seq(
  // sindi
  "org.scala-tools.sindi" %% "sindi-core" % version,
  compilerPlugin("org.scala-tools.sindi" %% "sindi-compiler" % version),
  // sindi-demo
  "org.scala-tools.sindi" %% "sindi-examples-demo" % version,
  // bowler
  "org.bowlerframework" %% "core" % "0.5.1",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.eclipse.jetty" % "jetty-server" % "7.4.5.v20110725" % "jetty",
  "org.eclipse.jetty" % "jetty-webapp" % "7.4.5.v20110725" % "jetty"
)}

seq(webSettings:_*)

resolvers ++= Seq(
  "Scala-Tools Maven2 Releases" at "http://scala-tools.org/repo-releases",
  "Scala-Tools Maven2 Snapshots" at "http://scala-tools.org/repo-snapshots",
  "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

name := "integration-bowler"

version := "0.2-SNAPSHOT"

organization := "org.scala-tools.sindi.examples"

scalaVersion := "2.9.0-1"

scalacOptions += "-unchecked"

crossScalaVersions := Seq("2.9.0-1", "2.9.1.RC2")


libraryDependencies ++= Seq(
  "org.scala-tools.sindi" %% "sindi" % "0.2-SNAPSHOT",
  "org.scala-tools.sindi.examples" %% "demo" % "0.2-SNAPSHOT",
  "org.bowlerframework" %% "core" % "0.4.2",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.eclipse.jetty" % "jetty-server" % "7.4.5.v20110725" % "jetty",
  "org.eclipse.jetty" % "jetty-webapp" % "7.4.5.v20110725" % "jetty"
)

seq(webSettings:_*)


resolvers ++= Seq(
  "Scala-Tools Maven2 Releases" at "http://scala-tools.org/repo-releases",
  "Scala-Tools Maven2 Snapshots" at "http://scala-tools.org/repo-snapshots",
  "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)


autoCompilerPlugins := true

addCompilerPlugin("org.scala-tools.sindi" %% "sindi" % "0.2-SNAPSHOT")

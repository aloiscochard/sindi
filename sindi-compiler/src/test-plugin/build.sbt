name := "sindi-compiler-check"

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

libraryDependencies <+= scalaVersion(_ match {
    case "2.9.1" => ("org.specs2" %% "specs2" % "1.6.1" % "test")
    case _ => ("org.specs2" %% "specs2" % "1.5" % "test")
  }
)

resolvers ++= Seq(
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)


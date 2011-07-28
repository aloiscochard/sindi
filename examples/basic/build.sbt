name := "examples.basic"

version := "0.1"

organization := "org.scala-tools.sindi"

scalaVersion := "2.9.0"

libraryDependencies += "org.scala-tools.sindi" %% "sindi" % "0.1-SNAPSHOT"

autoCompilerPlugins := true

addCompilerPlugin("org.scala-tools.sindi" %% "sindi" % "0.1-SNAPSHOT")

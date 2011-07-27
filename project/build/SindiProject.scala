import sbt._

class SindiProject(info: ProjectInfo) extends DefaultProject(info) {
  val specs2         = "org.specs2" %% "specs2" % "1.3" % "test"

  lazy val ScalaToolsReleasesRepo   = MavenRepository("Scala-Tools Releases",
                                      "http://nexus.scala-tools.org/content/repositories/releases/")
  lazy val SonatypeReleasesRepo     = MavenRepository("Sonatype OSS releases",
                                      "http://oss.sonatype.org/content/repositories/releases")

  override def compileOptions = super.compileOptions ++ Seq(Unchecked)
  override def testFrameworks = super.testFrameworks ++
                                    Seq(new TestFramework("org.specs2.runner.SpecsFramework"))


  ////////////////
  // Publishing //
  ////////////////

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/"
  Credentials(Path.userHome / "Development" / "configurations" / ".credentials", log)
}

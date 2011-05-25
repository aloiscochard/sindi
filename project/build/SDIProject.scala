import sbt._

class SDIProject(info: ProjectInfo) extends DefaultProject(info) {
  override def compileOptions = super.compileOptions ++ Seq(Unchecked)

  val specs2      = Dependencies.specs2

  override def testFrameworks = super.testFrameworks ++
                                    Seq(new TestFramework("org.specs2.runner.SpecsFramework"))

  object Dependencies {
    lazy val specs2         = "org.specs2" %% "specs2" % "1.3" % "test"
  }

  object Repositories {
    lazy val ScalaToolsReleasesRepo = MavenRepository("Scala-Tools Releases",
                                        "http://nexus.scala-tools.org/content/repositories/releases/")
  }
}

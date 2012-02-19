import java.io.File
import scala.util.control.Exception._

import sbt._
import Keys._

object build extends Build {
  val check = TaskKey[Unit]("check")
  lazy val root = Project("main", file("."), settings = Defaults.defaultSettings ++ Seq(
      watchSources <+= baseDirectory.map(_ / "check"),
      check <<= (
        baseDirectory, 
        fullClasspath in Compile, 
        compilers in Compile, 
        scalacOptions in Compile,
        streams,
        cacheDirectory
      ).map(checkTask)
    )
  )

  private def checkTask(
    root: File,
    classpath: Classpath,
    compilers: Compiler.Compilers,
    scalacOptions: Seq[String],
    streams: TaskStreams,
    cache: File): Unit = 
  {
    var success = true

    ((root / "check") ** "*.scala").get.foreach { source =>
      val name = source.getName
      val fail = new File(source.getPath + ".fail")
      val failExists = fail.exists
      val logger = new CheckLogger(source.getPath)

      def valid = streams.log.success(name)

      def error(expected: String, logger: String) = {
        success = false
        streams.log.error(name + ": failure mismatch\n----FOUND----\n" + logger + "\n----EXPECTED----\n" + expected)
      }

      def errorUnsuccess(logger: String) = {
        success = false
        streams.log.error(name + ": compiled unsuccessfuly\n" + logger)
      }

      def errorSuccess = {
        success = false
        streams.log.error(name + ": compiled successfuly")
      }

      val compiled = allCatch.opt{
        compilers.scalac.apply(
          Seq(source),
          classpath.map(_.data),
          IO.temporaryDirectory,
          scalacOptions, 
          new Callback, 
          1024, 
          logger
        )
      }.isDefined
      
      (compiled, failExists) match {
        case (false, true) => {
          val failString = io.Source.fromFile(fail).getLines.mkString("\n")
          val loggerString = logger.mkString
          if (failString.equals(loggerString)) valid
          else error(failString, loggerString)
        }
        case (true, false) => valid
        case (false, false) => errorUnsuccess(logger.mkString)
        case (true, true) => errorSuccess
      }
    }

    if (!success) throw new Error("checking failure")
  }
}

class CheckLogger(filePath: String) extends Logger {
  def trace(t: => Throwable): Unit = {}
  def success(message: => String): Unit = {}
  def log(level: Level.Value, message: => String): Unit = level match {
    case Level.Error => {
      if (!builder.isEmpty) builder.append("\n")
      builder.append(message.replace(filePath, ""))
    }
    case _ => 
  }

  def mkString = builder.mkString

  private val builder = new StringBuilder
}

import xsbti.AnalysisCallback
import xsbti.api.SourceAPI

class Callback extends AnalysisCallback {
  def api(sourceFile: File, source: SourceAPI): Unit = {}
  def beginSource(source: File): Unit = {}
  def binaryDependency(binary: File, name: String, source: File): Unit = {}
  def endSource(sourcePath: File): Unit = {}
  def generatedClass(source: File, module: File, name: String): Unit = {}
  def sourceDependency(dependsOn: File, source: File): Unit = {}
}

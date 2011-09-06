//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi
package compiler 

import scala.annotation.tailrec

import scala.tools.nsc 
import nsc.Global 
import nsc.Phase 
import nsc.ast.TreeBrowsers
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

class CompilerPlugin(override val global: Global) extends validator.ValidatorPlugin(global) {
  import global._

  val name = "sindi"
  val description = "Sindi Compiler"
  val components = List[PluginComponent](Analyze, Validate)
  override var options = Options()
  override val optionsHelp = Some(Options.help)

  override def processOptions(o: List[String], error: String => Unit) = {
    Options.apply(o) match {
      case Right(o) => options = o
      case Left(e) => error(e)
    }
  }

  trait Component extends utils.ParallelPluginComponent {
    val global: CompilerPlugin.this.global.type = CompilerPlugin.this.global
    val pluginName = CompilerPlugin.this.name
    def options = CompilerPlugin.this.options
  }


  object Analyze extends Component {
    val runsAfter = List[String]("refchecks")
    val phaseName = pluginName + "-analyze"
    def newPhase(_prev: Phase) = new AnalyzePhase(_prev)

    class AnalyzePhase(prev: Phase) extends ParallelPhase(prev) {
      override def name = Analyze.phaseName
      val registry = new RegistryWriter

      def async(unit: CompilationUnit, body: Tree) = {
        read(unit, body, registry)
      }
    }
  }

  object Validate extends Component {
    val runsAfter = List[String](Analyze.phaseName)
    val phaseName = pluginName + "-validate"
    def newPhase(_prev: Phase) = new ValidatePhase(_prev)

    class ValidatePhase(prev: Phase) extends ParallelPhase(prev) {
      override def name = Validate.phaseName

      def async(unit: CompilationUnit, body: Tree) = prev match {
        case phase: Analyze.AnalyzePhase => check(unit, phase.registry.toReader)
        case _ => throw new RuntimeException("Phase '" + name + "' isn't after phase '" + Analyze.phaseName + "'.")
      }
    }
  }
}

trait Component {
  val global: Global
  def options: Options
}

case class Options(val verbose: Boolean = false)

private object Options {
  val default = Options()

  def apply(options: List[String]): Either[String, Options] = {
    var oVerbose = default.verbose
    for (option <- options) {
      option match {
        case "verbose" => oVerbose = true
        case _ => return Left("Option not understood: " + option)
      }
    }
    Right(new Options(verbose = oVerbose))
  }

  def help = """
      -P:sindi:verbose           show compiler informations
  """
}

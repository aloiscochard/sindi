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

class CompilerPlugin(override val global: Global) extends checker.CheckerPlugin(global) {
  import global._

  val name = "sindi"
  val description = "Sindi Compiler"
  val components = List[PluginComponent](Read, Check)
  var options: Option[Options] = None

  trait Component extends utils.ParallelPluginComponent {
    val global: CompilerPlugin.this.global.type = CompilerPlugin.this.global
    val pluginName = CompilerPlugin.this.name
    def options = _options.getOrElse(Options())
    private val _options: Option[Options] = CompilerPlugin.this.options
  }

  override val optionsHelp: Option[String] = Some(Options.help)

  object Read extends Component {
    val runsAfter = List[String]("refchecks")
    val phaseName = pluginName + "-read"
    def newPhase(_prev: Phase) = new ReadPhase(_prev)

    class ReadPhase(prev: Phase) extends ParallelPhase(prev) {
      override def name = Check.phaseName
      val registry = new RegistryWriter

      def async(unit: CompilationUnit, body: Tree) = {
        println("async: " + unit);
        read(unit, body, registry)
      }
    }
  }

  object Check extends Component {
    val runsAfter = List[String](Read.phaseName)
    val phaseName = pluginName + "-check"
    def newPhase(_prev: Phase) = new CheckPhase(_prev)

    class CheckPhase(prev: Phase) extends ParallelPhase(prev) {
      override def name = Check.phaseName

      def async(unit: CompilationUnit, body: Tree) = prev match {
        case phase: Read.ReadPhase => check(unit, phase.registry.toReader)
        case _ => throw new RuntimeException("Phase '" + name + "' isn't after phase '" + Read.phaseName + "'.")
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


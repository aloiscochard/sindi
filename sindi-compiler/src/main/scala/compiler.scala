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

import reader.Reader
import checker.Checker
import model.Registry

class CompilerPlugin(val global: Global) extends Plugin {
  import global._

  val name = "sindi"
  val description = "Sindi Compiler"
  val components = List[PluginComponent](Read, Check)
  var options: Option[Options] = None

  trait Component extends PluginComponent {
    val global: CompilerPlugin.this.global.type = CompilerPlugin.this.global
    val pluginName = CompilerPlugin.this.name
  }

  override def processOptions(o: List[String], error: String => Unit) {
    options = Options(o) match {
      case Left(options) => Some(options)
      case Right(message) => error(message); None
    }
  }

  override val optionsHelp: Option[String] = Some(Options.help)

  object Read extends Component with Reader {
    class ReadPhase(prev: Phase) extends StdPhase(prev) {
      override def name = Check.phaseName
      var registry: Option[Registry] = None

      def apply(unit: CompilationUnit) { registry = read(unit) }
    }

    val runsAfter = List[String]("refchecks")
    val phaseName = pluginName + "-read"
    def newPhase(_prev: Phase) = new ReadPhase(_prev)
  }

  object Check extends Component with Checker {
    class CheckPhase(prev: Phase) extends StdPhase(prev) {
      override def name = Check.phaseName

      def apply(unit: CompilationUnit) {
        prev match {
          case phase: Read.ReadPhase => phase.registry match {
            case Some(registry) => check(unit, registry)
            case _ => throw new RuntimeException("Registry is empty, cannot check compilation unit.")
          }
          case _ => throw new RuntimeException("Phase '" + name + "' isn't after phase '" + Read.phaseName + "'.")
        }
      }
    }

    val runsAfter = List[String](Read.phaseName)
    val phaseName = pluginName + "-check"
    def newPhase(_prev: Phase) = new CheckPhase(_prev)
  }
}

trait Component { val global: Global }

case class Options(val verbose: Boolean)

private object Options {
  def apply(options: List[String]): Either[Options, String] = {
    var oVerbose = false
    for (option <- options) {
      option match {
        case "verbose" => oVerbose = true
        case _ => return Right("Option not understood: " + option)
      }
    }
    Left(new Options(verbose = oVerbose))
  }

  def help = """
      -P:sindi:verbose           show compiler informations
  """
}

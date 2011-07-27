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

import scala.tools.nsc
import nsc.Global
import nsc.Phase
import nsc.ast.TreeBrowsers
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

class DependencyChecker(val global: Global) extends Plugin {
  import global._

  val name = "sindi.depchecks"
  val description = "Checks if Sindi's modules dependencies are satisfated"
  val components = List[PluginComponent](Component)

  private object Component extends PluginComponent {
    val global: DependencyChecker.this.global.type = DependencyChecker.this.global
    val runsAfter = List[String]("refchecks")
    val phaseName = DependencyChecker.this.name

    def newPhase(_prev: Phase) = new DependencyCheckerPhase(_prev)

    class DependencyCheckerPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DependencyChecker.this.name

      def apply(unit: CompilationUnit) {
        // allows to browse the whole AST
        DependencyChecker.this.global.treeBrowsers.create().browse(unit.body)
      }
    }
  }
}

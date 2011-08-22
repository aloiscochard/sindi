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

// TODO [aloiscochard] Checking imported module when mixing components:
// If a component is mixed in from an out of scope module, check if module is imported

class SindiPlugin(val global: Global) extends Plugin {
  import global._

  trait Level
  class Warn extends Level
  class Error extends Level

  val name = "sindi"
  val description = "Sindi"
  val components = List[PluginComponent](Component)

  var debug = false
  var optionIgnore = true
  var boundEnabled = true
  var scopeEnabled = true
  var boundLevel: Level = new Error
  var scopeLevel: Level = new Warn
  
  override def processOptions(options: List[String], error: String => Unit) {
    for (option <- options) {
      option match {
        case "debug" => debug = true
        case "option" => optionIgnore = false
        case "scope.disable" => scopeEnabled = false
        case "scope.error" => scopeLevel = new Error
        case "bound.disable" => boundEnabled = false
        case "bound.warn" => boundLevel = new Warn
        case _ => error("Option not understood: " + option)
      }
    }
  }

  override val optionsHelp: Option[String] = Some("""
      -P:sindi:debug             show debug informations
      -P:sindi:option            check if scala.Option based bindings are satisfied
      -P:sindi:scope.disable     disable out of scope binding validation
      -P:sindi:scope.error       error instead of warn when an out of scope binding is found
      -P:sindi:bound.disable     disable component binding validation
      -P:sindi:bound.warn        warn instead of error when a component's binding isn't bound
    """)

  private object Component extends SindiComponent {
    val global: SindiPlugin.this.global.type = SindiPlugin.this.global
    val runsAfter = List[String]("refchecks")
    val phaseName = SindiPlugin.this.name

    def newPhase(_prev: Phase) = new SindiPluginPhase(_prev)

    class SindiPluginPhase(prev: Phase) extends StdPhase(prev) {
      override def name = SindiPlugin.this.name

      def apply(unit: CompilationUnit) {
        def notify(level: Level, message: String, tree: Tree) = level match {
          case level: Warn => unit.warning(tree.pos, message)
          case _ => unit.error(tree.pos, message)
        }

        def notifyBound(dependency: Dependency) = {
          if (boundEnabled) {
            val (module, injected, tree) = dependency
            notify(boundLevel, "type not bound\n\ttype: '%s'\n\tmodule: '%s'".format(injected, module), tree)
          }
        }

        def notifyScope(dependency: Dependency) = {
          if (scopeEnabled) {
            val (module, injected, tree) = dependency
            notify(scopeLevel,
                  "injecting from an out of scope module\n\ttype: '%s'\n\tmodule: '%s'".format(injected, module), tree)
          }
        }
        
        val (contexts, modules, components) = filter(unit.body)

        if (debug) {
          print(contexts.map((c) => { "[sindi.debug] <-> %s\n".format(c.toString) }).mkString)
          print(modules.map((m) => { "[sindi.debug] --> %s\n".format(m.tree.name.toString) }).mkString)
          print(components.map((c) => { "[sindi.debug] <-- %s\n".format(c.toString) }).mkString)
          //global.treeBrowsers.create().browse(tree)
        }

        // Validating components
        for (component <- components;
             dependency <- component.dependencies) {
          val (module, injected, tree) = dependency
          // Optionaly fiter options
          if (!(optionIgnore && isOption(injected))) {
            modules.find((m) => isAssignable(m.tree.symbol.tpe, module)) match {
              case Some(m) => {
                m.bindings.find((b) => isAssignable(injected, b)) match {
                  case Some(b) => 
                  case _ => notifyBound(dependency)
                }
              }
              case _ => notifyScope(dependency)
            }
          }
        }

        // Validating contexts
        for (context <- contexts) {
          for (dependency <- context.dependencies) {
            val (module, injected, tree) = dependency
            context.modules.find((m) => isAssignable(m, module)) match {
              case Some (m) =>
              case None => notifyScope(dependency)
            }
          }
          //global.treeBrowsers.create().browse(context.tree)
        }
      }

      // FIXME [aloiscochard] this horrible hack should be removable with upcoming new reflection library
      private def isOption(to: Type): Boolean = {
        to.baseClasses.foreach((symbol) => { if (symbol.tpe.toString == "Option[A]") return true })
        false
      }

    }
  }
}


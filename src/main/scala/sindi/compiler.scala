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

// Remarks: not sure check must be done with imported modules, would make sense to force explicit binding of them.

// TODO [acochard] Check for injection on imported modules (transitive must be explicit)
// TODO [acochard] Warn when dependencies can't be checked
// TODO [acochard] Add options to configure warning/error level of checks

class ModuleValidator(val global: Global) extends Plugin {
  import global._

  val name = "sindi"
  val description = "Sindi Modules Validator"
  val components = List[PluginComponent](Component)

  var debug = false
  
  override def processOptions(options: List[String], error: String => Unit) {
    for (option <- options) {
      option match {
        case "debug" => debug = true
        case _ => error("Option not understood: " + option)
      }
    }
  }
  
  override val optionsHelp: Option[String] = Some(
    "  -P:sindi:debug              show debug informations")

  private object Component extends PluginComponent {
    val global: DependencyChecker.this.global.type = DependencyChecker.this.global
    val runsAfter = List[String]("refchecks")
    val phaseName = DependencyChecker.this.name

    def newPhase(_prev: Phase) = new DependencyCheckerPhase(_prev)

    class DependencyCheckerPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DependencyChecker.this.name

      def apply(unit: CompilationUnit) {
        val (modules, components) = filter(unit.body)

        if (debug) {
          println(modules.map((m) => { "[sindi.debug] --> %s".format(m.toString) }).mkString("\n"))
          println(components.map((c) => { "[sindi.debug] <-- %s".format(c.toString) }).mkString("\n"))
        }

        for (component <- components;
             dependency <- component.dependencies) {
           val (module, injected, tree) = dependency
           modules.find((m) => m.tree.symbol.tpe == module) match {
             case Some(m) => {
               m.bindings.find((b) => b == injected) match {
                 case Some(b) => 
                 case _ =>
                   unit.error(tree.pos, "type not bound\n\ttype: '%s'\n\tmodule: '%s'".format(injected, module))
               }
             }
             case _ => {
               unit.warning(tree.pos, "injecting from a module not in scope should be avoided")
               // Component use a non defined module
               // TODO Check in imported module, if present otherwise error ?
             }
           }
        }
        //DependencyChecker.this.global.treeBrowsers.create().browse(tree) 
      }

      private type Bindings = List[Type]
      private type Dependencies = List[Tuple3[Type, Type, Tree]]

      private case class Module(tree: ClassDef, bindings: Bindings) {
        override def toString = tree.name + ":\n\t\t\t" + bindings.mkString("\n\t\t\t")
      }

      private case class Component(tree: ClassDef, dependencies: Dependencies) {
        override def toString = tree.name + ":\n\t\t\t" + dependencies.map((d) => { d._1 + " -> " + d._2}).mkString("\n\t\t\t")
      }

      private def filter(body: Tree): (List[Module], List[Component]) = {
        val entities: List[Either[Module, Component]] = (for (tree @ ClassDef(_, _, _, _) <- body) yield {
          if (isModule(tree)) {
            Some(Left(Module(tree, getBindings(tree))))
          } else if (isComponent(tree)) {
            Some(Right(Component(tree, getDependencies(tree))))
          } else {
            None
          }
        }).flatten

        val modules: List[Module] = 
          entities.flatMap((e) => { e match { case Left(module) => Some(module); case _ => None } })

        val components: List[Component] = 
          entities.flatMap((e) => { e match { case Right(component) => Some(component); case _ => None } })

        (modules, components)
      }


      private def isComponent(tree: Tree) = implement[sindi.Component](tree)
      private def isModule(tree: Tree) = implement[sindi.Module](tree)

      private def implement[T : Manifest](tree: Tree): Boolean = {
        find((tree) => { tree.tpe.toString == manifest[T].erasure.getName }, tree).isDefined
      }

      private def getBindings(tree: ClassDef): Bindings = {
        var bindings = List[Type]()
        for (tree @ ValDef(_, _, _, _) <- tree.impl.body) {
          if (tree.tpt.tpe.toString == "List[sindi.binder.binding.Binding[_]]" ||
              tree.tpt.tpe.toString == "sindi.package.Bindings") {
            for (tree @ Apply(_, _) <- tree.children) {
              collect[TypeTree](tree.children)((tree) => {
                tree match {
                  case tree: TypeApply => {
                    if (tree.symbol.owner.toString == "trait DSL" && tree.symbol.name.toString == "bind") {
                      var f: Option[TypeTree] = None
                      for (t <- tree.children) {
                        t match {
                          case t: TypeTree => f = Some(t)
                          case _ => 
                        }
                      }
                      f
                    } else {
                      None
                    }
                  }
                  case _ => None
                }
              }).foreach((tree) => {
                bindings = tree.tpe :: bindings
              })
            }
          }
        }
        bindings
      }

      private def getDependencies(tree: Tree): Dependencies = {
          val dependencies: List[Option[Tuple3[Type, Type, Tree]]] = 
            for (tree @ DefDef(_, _, _, _, _, _) <- tree.children.head.children) yield {
              var injected: Type = null
              var module: Type = null
              find((tree) => {
                tree match {
                  case apply: Apply => 
                      apply.symbol.owner.toString == "trait Injector" && apply.symbol.name.toString == "inject"
                  case _ => false
                }
              }, tree) match {
                case Some(tree) => {
                  injected = tree.tpe
                  find((tree) => {
                    tree match {
                      case typeApply: TypeApply =>
                        typeApply.symbol.name.toString == "from"
                      case _ => false
                    }
                  }, tree) match {
                    case Some(tree) =>
                      find((tree) => {
                        tree match {
                          case typeTree: TypeTree => true
                          case _ => false
                        }
                      }, tree) match {
                        case Some(tree) => {
                          module = tree.tpe
                        }
                        case _ =>
                      }
                    case _ =>
                  }
                }
                case None =>
              }
              if (injected == null) { None } else { Some((module, injected, tree)) }
            }

          dependencies.flatten
      }

      private def find(matcher: (Tree) => Boolean, tree: Tree): Option[Tree] = {
        findA[Tree]((tree) => { if (matcher(tree)) { Some(tree) } else { None } }, tree)
      }

      private def findA[T <: Tree](filter: (Tree) => Option[T], tree: Tree): Option[T] = {
        val trees = collect[T](List(tree))(filter)
        if (trees.isEmpty) { None } else { Some(trees.head) }
      }

      @tailrec
      private def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
          (filter: (Tree) => Option[T]): List[T] = {
        var children = List[Tree]()
        val found = for(tree <- lookup) yield {
          children = children ++ tree.children
          filter(tree)
        }
        if (children.isEmpty) { accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
      }

    }
  }
}

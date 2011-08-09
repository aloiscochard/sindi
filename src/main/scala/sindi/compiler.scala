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

// TODO [acochard] Check for injection of non-imported modules (transitive must be explicit)

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
        (for (tree @ ClassDef(_, _, _, _) <- unit.body) yield {
          if (isModule(tree)) {
            Left((tree, getBindings(tree)))
          } else if (isComponent(tree)) {
            Right((tree, getDependencies(tree)))
          } else {
            None
          }
        }).foreach((entity) => {
          entity match {
            case Left((tree, bindings)) =>
            case Right((tree, dependencies)) =>
          }
        })

        for (tree @ ClassDef(_, _, _, _) <- unit.body) {
          
          if (isModule(tree)) {
            println("[module]" + tree.name)
            getBindings(tree).foreach((t) => {
              println("<- %s".format(t))
            })
          }
          if (isComponent(tree)) {
            println("[component]" + tree.name)
            getDependencies(tree).foreach((d) => {
              val (injected, module) = d
              println("%s -> %s".format(module, injected))
            })
          }
        }

        //DependencyChecker.this.global.treeBrowsers.create().browse(tree) 
      }

      private def getBindings(tree: ClassDef): List[Type] = {
        var bindings = List[Type]()
        for (tree @ ValDef(_, _, _, _) <- tree.impl.body) {
          if (tree.tpt.tpe.toString == "List[sindi.binder.binding.Binding[_]]" ||
              tree.tpt.tpe.toString == "sindi.package.Bindings") {
            for (tree @ Apply(_, _) <- tree.children) {
              // Collect all TypeTree, typeTree.tpe = Bound
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

      private def getDependencies(tree: Tree): List[Tuple2[Type, Type]] = {
          val dependencies: List[Option[Tuple2[Type, Type]]] = 
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
              if (injected == null || module == null) { None } else { Some((injected, module)) }
            }

          dependencies.flatten
      }

      private def isComponent(tree: Tree) = implement[sindi.Component](tree)
      private def isModule(tree: Tree) = implement[sindi.Module](tree)

      private def implement[T : Manifest](tree: Tree): Boolean = {
        find((tree) => {
          tree.tpe.toString == manifest[T].erasure.getName
        }, tree).isDefined
      }

      private def find(matcher: (Tree) => Boolean, tree: Tree): Option[Tree] = {
        findA[Tree]((tree) => {
          if (matcher(tree)) { Some(tree) } else { None }
        }, tree)
      }

      private def findA[T <: Tree](filter: (Tree) => Option[T], tree: Tree): Option[T] = {
        val trees = collect[T](List(tree))(filter)
        if (trees.isEmpty) {
          None
        } else {
          Some(trees.head)
        }
      }

      @tailrec private def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
          (filter: (Tree) => Option[T]): List[T] = {
        var children = List[Tree]()
        val found = for(tree <- lookup) yield {
          children = children ++ tree.children
          filter(tree)
        }
        if (children.isEmpty) {
          accumulator
        } else {
          collect(children, found.flatten ++ accumulator)(filter)
        }
      }

    }
  }
}

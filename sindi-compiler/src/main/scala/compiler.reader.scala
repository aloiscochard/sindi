//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi.compiler
package reader 

import scala.annotation.tailrec
import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import model.ModelPlugin

abstract class ReaderPlugin (override val global: Global) extends ModelPlugin(global) {
  import global._

  private lazy val symContext = global.definitions.getClass(manifest[sindi.Context].erasure.getName)
  private lazy val symComponent = global.definitions.getClass(manifest[sindi.Component[_]].erasure.getName)

  def read(unit: CompilationUnit, registry: RegistryWriter): Unit = {
    var contexts: List[Context] = Nil
    var components: List[Component] = Nil

    for (tree @ ClassDef(_, _, _, _) <- unit.body) {
      if (isContext(tree)) {
        contexts = createContext(tree) :: contexts
      } else if (isComponent(tree)) {
        //components = createComponent(tree) :: components
      }
    }

    registry += CompilationUnitInfo(unit, contexts, components)
  }

  protected def isContext(tree: Tree) = tree.symbol.isSubClass(symContext)
  protected def isComponent(tree: Tree) = tree.symbol.isSubClass(symComponent)

  private def createContext(tree: ClassDef): Context = {
    new Context(tree, getModules(tree), getBindings(tree), getDependencies(tree))
  }

  private def getModules(tree: ClassDef) = {
    collect[DefDef](tree.children)((tree) => {
      tree match {
        case tree: DefDef => if (tree.symbol.name.toString == "modules") Some(tree) else None
        case _ => None
      }
    }).headOption match {
      case Some(tree) => {
        collect[ValDef](tree.children)((tree) => {
          tree match {
            case tree: ValDef => Some(tree)
            case _ => None
          }
        }) map ((tree) => {
          tree.symbol.tpe
        })
      }
      case None => Nil
    }
  }

  protected def getBindings(tree: ClassDef): List[Binding] = {
    var bindings = List[Binding]()
    for (tree @ ValDef(_, _, _, _) <- tree.impl.body) {
      if (tree.tpt.tpe.toString.startsWith("List[sindi.binder.binding.Binding[") ||
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
          }).foreach((tree) => { bindings = Binding(tree,tree.tpe) :: bindings })
        }
      }
    }
    bindings
  }

  private def getDependencies(tree: Tree): List[Dependency] = {
    val dependencies: List[Option[Dependency]] = 
      collect[Tree](List(tree))((tree) => {
        tree match {
          case d: DefDef => Some(tree)
          case _ => None
        }
      }).map((tree) => {
        var injected: Type = null
        var module: Type = null
        find((tree) => {
          tree match {
            case apply: Apply => {
              apply.symbol.owner.toString == "trait Injector" && apply.symbol.name.toString == "inject"
            }
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
        if (injected == null) { None } else {
          Some(Dependency(tree, module, Some(Dependency(tree, injected, None))))
        }
      })
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
  private final def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
  }
}

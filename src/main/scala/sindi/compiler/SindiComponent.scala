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
import nsc.plugins.PluginComponent

trait SindiComponent extends PluginComponent {
  val global: Global
  import global._

  object Types {
    val module = global.definitions.getClass(classOf[sindi.Module].getName)
    val bindingList = global.definitions.getClass(classOf[List[sindi.binder.binding.Binding[_]]].getName)
    val bindings = global.definitions.getClass(classOf[sindi.`package`.Bindings].getName)
  }

  type Bindings = List[Type]
  type Dependencies = List[Tuple3[Type, Type, Tree]]

  case class Module(tree: ClassDef, bindings: Bindings) {
    override def toString = tree.name + ":\n\t\t\t" + bindings.mkString("\n\t\t\t")
  }

  case class Component(tree: ClassDef, dependencies: Dependencies) {
    override def toString = tree.name + ":\n\t\t\t" + dependencies.map((d) => { d._1 + " -> " + d._2}).mkString("\n\t\t\t")
  }

  def isAssignable(from: Type, to: Type) = from <:< to || from.toString == to.toString

  def filter(body: Tree): (List[Module], List[Component]) = {
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

  private def isComponent(tree: Tree) = {
    //global.treeBrowsers.create().browse(tree)
    find((tree) => { tree.tpe.toString == manifest[sindi.Component].erasure.getName }, tree).isDefined
  }

  private def isModule(tree: Tree) = {
    find((tree) => {
      tree.tpe.toString == manifest[sindi.Module].erasure.getName ||
      tree.tpe.toString.startsWith(manifest[sindi.ModuleT[_]].erasure.getName)
    }, tree).isDefined
  }


  private def getBindings(tree: ClassDef): Bindings = {
    var bindings = List[Type]()
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
          }).foreach((tree) => { bindings = tree.tpe :: bindings })
        }
      }
    }
    bindings
  }

  private def getDependencies(tree: Tree): Dependencies = {
    val dependencies: List[Option[Tuple3[Type, Type, Tree]]] = 
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
        if (injected == null) { None } else { Some((module, injected, tree)) }
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
  private def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
  }

}


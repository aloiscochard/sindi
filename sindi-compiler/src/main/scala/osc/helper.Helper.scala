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
package helper

import scala.annotation.tailrec

import scala.tools.nsc.Global 

trait Helper {
  val global: Global
  import global._

  type Bindings = List[Type]
  type Dependency = Tuple3[Type, Type, Tree]
  type Dependencies = List[Dependency]

  protected def find(matcher: (Tree) => Boolean, tree: Tree): Option[Tree] = {
    findA[Tree]((tree) => { if (matcher(tree)) { Some(tree) } else { None } }, tree)
  }

  protected def findA[T <: Tree](filter: (Tree) => Option[T], tree: Tree): Option[T] = {
    val trees = collect[T](List(tree))(filter)
    if (trees.isEmpty) { None } else { Some(trees.head) }
  }

  @tailrec
  protected final def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
  }

  protected def getDependencies(tree: Tree): Dependencies = {
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
}

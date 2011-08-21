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

import scala.tools.nsc.Global 

trait ComponentHelper extends Helper {
  import global._

  case class Component(tree: ClassDef) {
    val dependencies = getDependencies(tree)
    override def toString = tree.name + ":\n\t\t\t" + dependencies.map((d) => { d._1 + " -> " + d._2}).mkString("\n\t\t\t")
  }

  protected def isComponent(tree: Tree) = {
    //global.treeBrowsers.create().browse(tree)
    find((tree) => { tree.tpe.toString == manifest[sindi.Component].erasure.getName }, tree).isDefined
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
}

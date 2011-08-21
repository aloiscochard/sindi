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

trait ContextHelper extends Helper {
  import global._

  case class Context(tree: ClassDef) {
    val modules = getModules(tree)
    val bindings = getBindings(tree)
    override def toString = tree.name + ":\n\t\tmodules {" + modules.map("\n\t\t\t" + _).mkString  +
                            "\n\t\t}\n\t\tbindings {" + bindings.map("\n\t\t\t" + _).mkString + "\n\t\t}"
  }

  protected def isContext(tree: Tree) = {
    find((tree) => { tree.tpe.toString == manifest[sindi.Context].erasure.getName }, tree).isDefined
  }

  protected def getModules(tree: ClassDef) = {
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
          //global.treeBrowsers.create().browse(tree)
          //println("module -> " + tree)
          tree.symbol.tpe
        })
      }
      case None => Nil
    }
  }

  protected def getBindings(tree: ClassDef): Bindings = {
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
}

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
package analyzer 

import scala.tools.nsc
import nsc.Global 

trait ContextAnalyzis extends Analyzis {
  import global._

  protected def createContext(tree: ClassDef) =
    new Context(tree, getModules(tree), getBindings(tree), getDependencies(tree))

  protected def getModules(tree: ClassDef) = {
    collect[DefDef](tree.children)((tree) => tree match {
      case tree: DefDef => if (tree.name.toString == "modules") Some(tree) else None
      case _ => None
    }).headOption match {
      case Some(tree) => {
        collect[ValDef](tree.children)((tree) => tree match {
          case tree: ValDef => Some(tree)
          case _ => None
        }).map((tree) => {
          Module(tree.symbol, tree.symbol.tpe, tree.symbol.tpe.toString)
        })
      }
      case None => Nil
    }
  }

  private def getBindings(tree: ClassDef): List[Binding] = {
    def typeOf(tree: Tree) = tree.children.collectFirst { case t: TypeTree => t }
    def isSelectOfBind(tree: Tree) = isSelectOf(tree, "bind")
    def isSelectOfAs(tree: Tree) = isSelectOf(tree, "as")
    def isSelectOf(tree: Tree, name: String) = (tree, tree.children.headOption) match {
      case (_: TypeApply, Some(tree: Select)) => tree.name.toString.trim == name
      case _ => false
    }

    val backtrack = (tree: Tree) => tree match {
      case _ if isSelectOfAs(tree) => true
      case _ if isSelectOfBind(tree) => true
      case _ => false
    }
    // TODO does it work ok with val? (/!\ compiler crash)
    var bindings = List[Binding]()
    for (tree @ ValDef(_, _, _, _) <- tree.impl.body if tree.name.toString.trim == "bindings") {
      traversal(tree.children)(backtrack).flatMap(_ match {
        case t if isSelectOfAs(t) => Some(traverse(t).find(isSelectOfBind(_)).flatMap(typeOf(_)) -> typeOf(t).map(_.tpe))
        case t if isSelectOfBind(t) => Some(typeOf(t) -> None)
        case _ =>
          None
      }).flatMap(_ match {
        case (Some(tree), qualifier) => Some(Binding(tree, tree.symbol, qualifier))
        case _ => None
      }).foreach(b => { bindings = b :: bindings })
    }
    bindings
  }
}


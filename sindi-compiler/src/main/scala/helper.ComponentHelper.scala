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

  lazy val symComponent = global.definitions.getClass(manifest[sindi.Component[_]].erasure.getName)

  case class Component(tree: ClassDef) {
    val dependencies = getDependencies(tree)
    override def toString = tree.name + ":\n\t\t\t" + dependencies.map((d) => { d._1 + " -> " + d._2}).mkString("\n\t\t\t")
  }

  protected def isComponent(tree: Tree) = tree.symbol.isSubClass(symComponent)
}

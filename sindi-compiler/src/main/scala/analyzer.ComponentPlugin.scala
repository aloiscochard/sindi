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

abstract class ComponentPlugin (override val global: Global) extends ContextPlugin(global) {
  import global._

  protected def createComponent(tree: ClassDef) = {
    val context = if (tree.symbol.isSubClass(symComponentWithContext)) {
      find[String](List(tree))((tree) => tree match {
        case tree: TypeTree => {
          val typeName = tree.tpe.toString
          if (typeName.startsWith("sindi.ComponentWith")) Some(getTypeParam(typeName)) else None
        }
        case _ => None
      })
    } else None

    val dependencies = getDependencies(tree)

    context match {
      case Some(context) => new ComponentWithContext(tree, context, dependencies)
      case _ => new Component(tree, getComponentModules(tree), dependencies)
    }
  }

  private def getComponentModules(root: ClassDef) = 
    getTypeDependencies(root.symbol.classBound).map((s) => Module(global.definitions.getClass(s), s))

}

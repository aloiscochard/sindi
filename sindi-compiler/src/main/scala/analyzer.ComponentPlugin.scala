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
    // Is it a ComponentWithContext ?
    val context = if (tree.symbol.isSubClass(symComponentWith)) {
      find[String](List(tree))((tree) => tree match {
        case tree: TypeTree => {
          val typeName = tree.tpe.toString
          if (typeName.startsWith(symComponentWith.fullName)) Some(getTypeParam(typeName)) else None
        }
        case _ => None
      })
    } else None

    val dependencies = getDependencies(tree)

    context match {
      case Some(context) => new ComponentWithContext(tree, context, dependencies)
      case _ => {
        val modules = {
          val modules = getComponentModules(tree)
          if (options.componentAutoImport) {
            // Add infered dependency (ManifestModule will be added in transform phase)
            val unresolved = dependencies.filter(_.symbol.isSubClass(symModule))
                              .filter((d) => modules.find(_.symbol == d.symbol).isEmpty)
            modules ++ unresolved.map((dependency) => {
              Module(dependency.symbol, dependency.symbol.name.toString, Some(dependency))
            })
          } else modules
        }
        new Component(tree, modules, dependencies)
      }
    }
  }

  private def getComponentModules(root: ClassDef) = 
    getTypeDependencies(root.symbol.classBound).map((s) => Module(global.definitions.getClass(s), s))

}



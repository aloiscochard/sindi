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

trait ModuleHelper extends ContextHelper {
  import global._

  lazy val symModule = global.definitions.getClass(manifest[sindi.Module].erasure.getName)
  lazy val symModuleT = global.definitions.getClass(manifest[sindi.ModuleT[_]].erasure.getName)

  protected def isModule(tree: Tree) = tree.symbol.isSubClass(symModule) || tree.symbol.isSubClass(symModuleT)
}


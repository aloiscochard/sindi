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

  protected def isModule(tree: Tree) = {
    find((tree) => {
      tree.tpe.toString == manifest[sindi.Module].erasure.getName ||
      tree.tpe.toString.startsWith(manifest[sindi.ModuleT[_]].erasure.getName)
    }, tree).isDefined
  }


}


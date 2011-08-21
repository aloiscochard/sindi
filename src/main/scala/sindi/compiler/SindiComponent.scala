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


import scala.tools.nsc
import nsc.Global 
import nsc.plugins.PluginComponent


trait SindiComponent extends PluginComponent with helper.ContextHelper with helper.ModuleHelper with helper.ComponentHelper {
  val global: Global
  import global._

  def isAssignable(from: Type, to: Type) = from <:< to || from.toString == to.toString

  def filter(body: Tree): (List[Context], List[Context], List[Component]) = {
    var contexts: List[Context] = Nil
    var modules: List[Context] = Nil
    var components: List[Component] = Nil

    for (tree @ ClassDef(_, _, _, _) <- body) {
      if (isContext(tree)) {
        val context = Context(tree)
        contexts = context :: contexts
        if (isModule(tree)) {
          modules = context :: modules
        }
      } else if (isComponent(tree)) {
        components = Component(tree) :: components
      }
    }

    (contexts, modules, components)
  }
}


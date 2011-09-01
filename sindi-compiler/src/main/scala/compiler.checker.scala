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
package checker 

import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import reader.ReaderPlugin

abstract class CheckerPlugin(override val global: Global) extends ReaderPlugin(global) {
  import global._

  def check(unit: CompilationUnit, registry: RegistryReader) = {
    registry(unit.source) match {
      case Some(info) => {
        (info.contexts ++ info.components).par.foreach((entity) => {
          println(entity.getClass)
          (entity match {
            case c: Context => Some(c)
            case Component(_, Some(module), _) => {
              val context = registry.getContext(module.classBound)
              println("\ncontext found: " + context)
              context
            }
            case _ => None
          }) match {
            case Some(context) => entity.dependencies.par.foreach((dependency) => {
              resolve(context, dependency)
            })
            case None => 
          }
        })
      }
      case _ =>
    }
  }

  private def resolve(context: Context, dependency: Dependency) = {
  }
}


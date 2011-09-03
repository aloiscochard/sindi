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

  // TODO [aloiscochard] more detailed error message ?
  private case class DependencyNotBound(dependency: Dependency) extends Failure {
    override def tree = dependency.tree
    override def message = "type not bound: '%s'".format(dependency.name)
  }

  private case class DependencyOutOfScope(dependency: Dependency) extends Failure {
    override def tree = dependency.tree
    override def message = "module out of scope: '%s'".format(dependency.name)
  }

  /*
  private case class ComponentOutOfScope(component: Component) extends Failure {
    override def tree = component.tree
    override def message = "component's module out of scope: '%s'".format(component.module)
  }
  */

  private trait Failure {
    def tree: Tree
    def message: String
  }

  def check(unit: CompilationUnit, registry: RegistryReader) = {
    def notify(failure: Failure) = global.synchronized {
      unit.error(failure.tree.pos, failure.message)
    }

    val resolver = resolve(registry)_
    registry(unit.source) match {
      case Some(info) => {
        (info.contexts ++ info.components).par.foreach((entity) => {
          (entity match {
            case context: Context => Some(context)
            case component: Component => {//Component(_, Some(module), _) => {
              //registry.getContext(module) 
              // TODO Recompose virtual context
              None
            }
            case _ => None
          }) match {
            case Some(context) => {
              entity.dependencies.par.flatMap((dependency) => {
                  /*
                entity match {
                  case component: Component => {
                    if (dependency.symbol != component.module.get) {
                      Some(DependencyOutOfScope(dependency))
                    } else {
                      dependency.dependency match {
                        case Some(dependency) => resolver(context, dependency)
                        case _ => None
                      }
                    }
                  }
                  case _ => resolver(context, dependency)
                }
                  */
                resolver(context, dependency)
              }).foreach(notify(_))
            }
            case None => entity match {
              case component: Component => //notify(ComponentOutOfScope(component))
              case _ => throw new RuntimeException("Impossible case detected during context analysis!")
            }
          }
        })
      }
      case _ =>
    }
  }

  private def resolve(registry: RegistryReader)(context: Context, dependency: Dependency): Option[Failure] = {
    context.modules.find(_.typeSymbol == dependency.symbol) match {
      case Some(moduleType) => {
        dependency.dependency match {
          case Some(dependency) => {
            registry.getContext(moduleType.typeSymbol) match {
              case Some(module) => resolve(registry)(module, dependency)
              case None => Some(DependencyOutOfScope(dependency))
            }
          }
          case _ => None
        }
      }
      case None => {
        context.bindings.find(_.symbol == dependency.symbol) match {
          case Some(binding) => None
          case None => Some(DependencyNotBound(dependency))
        }
      }
    }
  }
}


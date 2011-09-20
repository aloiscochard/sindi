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
package validator 

import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import transformer.TransformerPlugin

abstract class ValidatorPlugin(override val global: Global) extends TransformerPlugin(global) {
  import global._

  private final val symOption = global.definitions.getClass(manifest[Option[_]].erasure.getName)

  // TODO [aloiscochard] more detailed error message ?
  private case class DependencyNotBound(dependency: Dependency) extends Failure {
    override def tree = dependency.tree
    override def message = "type not bound: '%s'".format(dependency.name)
  }

  private case class DependencyOutOfScope(dependency: Dependency) extends Failure {
    override def tree = dependency.tree
    override def message = "module out of scope: '%s'".format(dependency.name)
  }
  
  private case class ContextOutOfScope(component: ComponentWithContext) extends Failure {
    override def tree = component.tree
    override def message = "context out of scope: '%s'".format(component.context)
  }

  private trait Failure {
    def tree: Tree
    def message: String
  }

  def check(unit: CompilationUnit, registry: RegistryReader) = {
    def notify(failure: Failure) = global.synchronized {
      unit.error(failure.tree.pos, failure.message)
    }

    def validate(entity: Entity, dependencies: List[Dependency]) =
      dependencies.par.flatMap((dependency) => resolve(registry)(entity, dependency)).foreach(notify(_))

    registry(unit.source) match {
      case Some(info) => {
        (info.contexts ++ info.components).par.foreach((entity) => {
          //global.treeBrowsers.create().browse(entity.tree)
          entity match {
            case component: ComponentWithContext => {
              registry.getContext(component.context) match {
                case Some(context) => validate(context, entity.dependencies)
                case _ => notify(ContextOutOfScope(component))
              }
            }
            case _ => validate(entity, entity.dependencies)
          }
        })
      }
      case _ =>
    }
  }

  private def resolve(registry: RegistryReader)(entity: Entity, dependency: Dependency): Option[Failure] = {
    // TODO: Configurable exclude filter
    // Filtering excluded types
    if (dependency.symbol == symOption) {
      None
    } else {
      // Resolving
      entity.modules.find(_.symbol == dependency.symbol) match {
        case Some(module) => {
          dependency.dependency match {
            case Some(dependency) => {
              registry.getContext(module.symbol) match {
                case Some(module) => resolve(registry)(module, dependency)
                case None => Some(DependencyOutOfScope(dependency))
              }
            }
            case _ => None
          }
        }
        case None => {
          entity.bindings.find(_.symbol == dependency.symbol) match {
            case Some(binding) => None
            case None => Some(DependencyNotBound(dependency))
          }
        }
      }
    }
  }
}


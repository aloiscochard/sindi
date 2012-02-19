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

trait Validator extends SindiPlugin {
  import global._

  private final val symOption = global.definitions.getClass(manifest[Option[_]].erasure.getName)
  private final val symEither = global.definitions.getClass(manifest[Either[_, _]].erasure.getName)

  // TODO [aloiscochard] more detailed error message ?
  private case class DependencyNotBound(dependency: Dependency) extends Failure {
    override def tree = dependency.tree
    override def message = "type not bound: '%s'".format(dependency.name) + (if (dependency.qualifiers.isEmpty) "" else
      " (as " + dependency.qualifiers.map(x => "'" + x + "'").mkString(" or ") + ")")
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
    // TODO: Show and point qualifier on error
    def isBound(d: Dependency)(b: Binding): Boolean = d.symbol == b.symbol && ((d.qualifiers, b.qualifier) match {
      case (Nil, None) => true
      case (Nil, Some(_)) => false
      case (qs, _) if qs.contains(symNone.tpe) => true
      case (qs, None) => false
      case (qs, Some(q)) => qs.contains(q)
    })

    if (dependency.symbol == symOption) {
      // TODO: Configurable exclude filter
      None 
    } else if (dependency.symbol == symEither) {
      // Either
      dependency.tree.tpe.typeArgs match {
        case left :: right :: Nil =>
          List(right, left).map((tpe) => 
              dependency.copy(signature = Signature(tpe.typeSymbol), name = tpe.typeSymbol.name.toString))
                    .flatMap(resolve(registry)(entity, _)) match {
            case left :: right :: Nil => Some(left)
            case any :: Nil => None
            case _ => None
          }
        case _ => throw new RuntimeException("Either dependency have wrong number of type parameter.\n" + 
                                             "It looks like some fundamental laws are broken, " +
                                             "please fix them... tips: '%s'".format(dependency.symbol.typeParams))
      }
    } else {
      // Resolving
      entity.modules.find((module) => {
        dependency.signature.tpe match {
          case Some(tpe) => module.tpe <:< tpe
          case None => module.symbol.isSubClass(dependency.symbol)
        }
      }) match {
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
          entity.bindings.find(isBound(dependency) _) match {
            case Some(binding) => None
            case None => Some(DependencyNotBound(dependency))
          }
        }
      }
    }
  }
}


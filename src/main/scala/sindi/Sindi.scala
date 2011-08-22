//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

// TODO [aloiscochard] Add assertions (security checks)
// TODO [aloiscochard] Improve Provider handling/implementation

/** Sindi IoC Container APIs.
  *
  * Provides facilities to declare/consume: [[sindi.Module]], [[sindi.Component]] and [[sindi.Context]]
 **/
package object sindi {
  /** A list of bindings.*/
  type Bindings = List[binder.binding.Binding[AnyRef]]
  /** A list of processors.*/
  type Processors[T <: AnyRef] = List[processor.Processor[T]]
  /** A list of modules.*/
  type Modules = List[Module]
}
package sindi {

  /** Bindings companion **/
  object Bindings {
    /** Create a new list of bindings **/
    def apply(bindings: binder.binding.Binding[_ <: AnyRef]*): List[binder.binding.Binding[AnyRef]] = {
      bindings.toList.asInstanceOf[List[binder.binding.Binding[AnyRef]]]
    }
  }

  object Modules { def apply(modules: Module*): Modules = modules.toList }

  /** Context who contain bindings informations and dependencies to modules. */
  trait Context extends context.Context with binder.DSL with Contextual {
    implicit val `implicit` = this

    protected lazy val modules: Modules = Nil
    protected override def processing = super.processing :+ processor.option

    def from[M <: Module : Manifest]: Module = {
      modules.foreach((module) => { Helper.moduleOf[M](module) match {
        case Some(module) => return module
        case _ =>
      }})
      throw ModuleNotFoundException(manifest[M])
    }
  }

  abstract class Provider[T <: AnyRef : Manifest] extends binder.binding.provider.Provider[T] {
    override val signature = manifest[T]
  }

  abstract class Module(implicit context: Context) extends Context with context.Childified {
    override protected val parent = context
  }

  abstract class ModuleT[T <: Any : Manifest](context: Context) extends Module()(context) {
    val manifest = implicitly[Manifest[T]]
  }


  trait Component[M <: Module] extends Contextual

  trait ComponentWithContext extends Contextual {
    protected val context: Context
    protected def from[M <: Module : Manifest] = context.from[M]
  }

  abstract class ComponentContext(implicit context: Context) extends Contextual {
    protected def from[M <: Module : Manifest] = context.from[M]
  }

  case class ModuleNotFoundException(module: Manifest[_]) extends Exception(
    "Unable to inject from module %s: module not found.".format(module))

  case class TypeNotBoundException(message: String) extends Exception(message)

  private[sindi] trait Contextual {
    protected def from[M <: Module : Manifest]: Module
  }

  private object Helper {
    def moduleOf[M <: Module : Manifest](module: Module): Option[M] = {
      if (module.getClass == manifest[M].erasure) {
        val m = module match {
          case module: ModuleT[_] => {
            manifest[M].typeArguments.headOption match {
              case Some(typeManifest) => if (!(module.manifest <:< manifest[M].typeArguments.head)) return None
              case _ => return None
            }
          }
          case _ => 
        }
        Some(module.asInstanceOf[M])
      } else {
        None
      }
    }
  }
}

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
                    
// TODO [aloiscochard] Add assertion

// SINAP
// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Implement Event/Lifecycle system using processor

object `package` {
  type Bindings = List[binder.binding.Binding[_]]
  type Processors = List[processor.Processor[_]]
}

trait Context extends context.Context with binder.DSL {
  protected val modules: List[Module] = Nil

  protected override def processing = {
    super.processing :+ processor.option
  }

  def from[M <: Module : Manifest]: sindi.injector.Injector = {
    modules.foreach((module) => {
      utils.Reflection.moduleOf[M](module) match {
        case Some(module) => return module.injector
        case _ =>
      }
    })
    throw exception.ModuleNotFoundException(manifest[M].erasure)
  }
}

abstract class AbstractProvider[T <: AnyRef : Manifest] extends binder.binding.provider.Provider[T] {
  override val signature = manifest[T]
}

abstract class Provider[T <: AnyRef : Manifest] extends AbstractProvider[T] {
  override def provide[T <: AnyRef : Manifest]: T = get.asInstanceOf[T]
  def get: T
}

abstract class Module(implicit context: Context) extends Context with context.Childified {
  override protected val parent = context
  def apply[S <: AnyRef : Manifest](): S = inject[S]
  def apply[S <: AnyRef : Manifest](qualifier: AnyRef): S = injectAs[S](qualifier)
}

abstract class ModuleFactory[M <: Module : Manifest] {
  def apply(implicit context: Context): M = utils.Reflection.createModule[M](context)
}

trait Component { 
  protected def from[M <: Module : Manifest]: injector.Injector
}

class ComponentContext(val context: Context) extends Component {
  protected def from[M <: Module : Manifest] = context.from[M]
}

package exception {
  case class ModuleNotFoundException(module: Class[_]) extends Exception(
    "Unable to inject from module %s: module not found.".format(module.getName))
  case class TypeNotBoundException(message: String) extends Exception(message)
}

package utils {
  object Reflection {
    def createModule[M <: Module : Manifest](context: Context) = {
      (manifest[M].erasure.getConstructor(classOf[Context]).newInstance(context)).asInstanceOf[M]
    }

    def moduleOf[M <: Module : Manifest](module: Module): Option[M] = {
      if (module.getClass == manifest[M].erasure) {
        Some(module.asInstanceOf[M])
      } else {
        None
      }
    }
  }
}

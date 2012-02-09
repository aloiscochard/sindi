//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

// TODO [aloiscochard] Autowiring abstract component
// TODO [aloiscochard] Add alternative to ModuleT by generating manifest and boierplate in compiler
// TODO [aloiscochard] Add assertions (security checks)
// TODO [aloiscochard] Avoid usage of _.1 and _.2

/** Sindi IoC Container APIs.
 *
 * [[sindi.Module]] is used as the basic building block to design loosely coupled and reconfigurable librairies with Sindi.
 *
 * Modules are composable and can be consumed in [[sindi.Context]] and [[sindi.Component]].
 */
package object sindi {
  /** A sequence of bindings. */
  type Bindings = Seq[binder.binding.Binding[AnyRef]]
  /** A sequence of [[sindi.processor.Processor]]. */
  type Processors[T <: AnyRef] = Seq[processor.Processor[T]]
  /** A sequence of [[sindi.Module]]. */
  type Modules = Seq[Module]
  /** A companion object for [[sindi.Module]] list construction. */
  object Modules { def apply(modules: Module*): Modules = modules.toList }
  /** A module manifest store safely a module type reference. */
  class ModuleManifest[M <: Module : Manifest]
  /** Return a new module with given bindings (implicit context is defined as new module's parent). */
  def module(_bindings: Bindings)(implicit context: Context) = new Module { override val bindings = _bindings }
  /** Return a qualifier for the given type. */
  def qualifier[T <: Any : Manifest] = manifest[T]                            
  /** Implicit conversion to construct qualifiers from any reference. */
  implicit def any2qualifiers(q: AnyRef): injector.Qualifiers = injector.Qualifiers(q)
}

package sindi {
  /** Bindings companion. */
  object Bindings {
    /** Create a new list of bindings. */
    def apply(bindings: binder.binding.Binding[_ <: AnyRef]*): List[binder.binding.Binding[AnyRef]] =
      bindings.toList.asInstanceOf[List[binder.binding.Binding[AnyRef]]]
  }

 /** A context is a collection of bindings and modules, it contains operations to wire objects together.
  * 
  * When integrating modules into a host application, 
  * it's recommend to us this trait as a seemless integration layer with the container.
  *
  * === Embedded ===
  * When possible, integrating context as trait or class should be prefered (reusability is complete).
  *
  * {{{
  * trait Application with Context {
  *   override lazy val modules = new UserServiceModule :: Nil
  *   def startup() = from[UserServiceModule].start()
  * }
  * 
  * // Scala Application
  * object Bootstrap extends App {
  *   new ApplicationContext{}.startup()
  * }
  *
  * // Web Application
  * class WebApp extends Application {
  *   new ApplicationContext{}.startup()
  * }
  * }}}
  *
  * === Global ===
  * If the context need to be accessed globally it can be defined as singleton object.
  *
  * {{{
  * import sindi._
  *
  * object ApplicationContext {
  *   override lazy val modules = new UserServiceModule :: Nil
  *   def startup() = from[UserServiceModule].start()
  * }
  * }}}
  *
  * You can then create a base component linked with this context by extending the [[sindi.ComponentWith]] trait.
  *
  * @see [[sindi.ComponentContext]], [[sindi.ComponentWith]]
  */
  trait Context extends context.Context with context.Wirable with binder.DSL with Composable {
    /** This context as implicit value to ease modules integration. */
    implicit val `implicit` = this

    /** Return the list of [[sindi.Module]] associated with this context. */
    protected lazy val modules: Modules = Nil

    override def from[M <: Module : Manifest]: M = modules.view.flatMap(Helper.moduleOf[M](_)).headOption match {
      case Some(module) => module
      case _ => throw exception.ModuleNotFoundException(manifest[M])
    }

    override def processors: List[processor.Processor[_]] = processor.option :: processor.either :: Nil

    abstract override protected def wire[T <: AnyRef : Manifest]: Option[T] = super.wire[T].orElse {
      import scala.util.control.Exception._

      modules.view.flatMap((module) => {
        module.getClass.getDeclaredMethods.filter((m) => Manifest.classType[AnyRef](m.getReturnType) <:< manifest[T])
          .flatMap {
            case method if method.getParameterTypes.size == 0 => Some(method.invoke(module).asInstanceOf[T])
            case method => {
              // TODO [aloiscochard] Cache arguments injection / or recursive autowiring
              val values = method.getParameterTypes.toList.map(Manifest.classType[AnyRef](_))
                .flatMap((m) => catching(classOf[exception.TypeNotBoundException]).opt(inject(m)))
              if (values.size == method.getParameterTypes.size) { Some(method.invoke(module, values:_*).asInstanceOf[T]) }
              else None
            }
          }
      }).headOption
    }
  }

  /** An injection provider with signature configured using parameterized type. */
  abstract class Provider[T <: AnyRef : Manifest] extends provider.Provider[T] {
    override val signature = manifest[T]
  }

 /** A module is a collection of services which are configured using bindings.
   *
   * The bindings are overridden at runtime using parent's [[sindi.Context]].
   *
   * <b>It's recommended to:</b></br>
   * - Declare class as final and prefer [[sindi.Module]] composition and [[sindi.Component]] mixing over class inheritance.</br>
   * - Define an implicit [[sindi.Context]] provided as class constructor to ease contextual integration.
   *
   * {{{
   * final class UserModule(implicit context: Context) extends Module {
   *   override lazy val modules = new StoreModule[User] :: Nil
   *
   *   override val bindings: Bindings =
   *     bind[UserService] to autowire[DefaultUserService]
   *
   *   def users = inject[UserService]
   * }
   * }}}
   *
   * @see [[sindi.ModuleT]]
   */
  abstract class Module(implicit context: Context) extends Context with context.Childable {
    override protected val parent = context
  }

  /** A class to declare type parameterized module that can be consumed safely.
   *
   * <i>Due to type erasure it's techincally impossible to differentiate
   * module which are constructed with parameterized types in a given context.</i>
   *
   * As a workaround this class allow to safely declare type parameterized module
   * by using an implicit [[scala.reflect.Manifest]].
   *
   * {{{
   * final class StoreModule[T <: AnyRef](implicit manifest: Manifest[T], context: Context) extends ModuleT[T] {
   *   override val bindings: Bindings =
   *     bind[Store[T]] to new DefaultStore[T]
   *
   *   def store = inject[Store[T]]
   * }
   * }}}
   *
   */
  abstract class ModuleT[T <: Any](implicit manifest: Manifest[T], context: Context) extends Module()(context) {
    private[sindi] val _manifest = manifest
  }

  /** A marker trait for type importing modules. */
  trait Composable {
    /** Return an imported module for a given module's type. */
    protected def from[M <: Module : Manifest]: M
  }

  /** A component is a set of features based on module's services.
   * 
   * {{{
   * trait SecurityComponent extends Component {
   *   def login() = from[SecurityModule].authenticate()
   *   def users = from[UserModule].users
   * }
   * }}}
   *
   * Components can be consumed in different ways with contexts/modules,
   * offering flexible integration within the container.
   * 
   * [[sindi.ComponentContext]] can be used to compose them,
   * [[sindi.ComponentWith]] to integrate them with a global context,
   * or they can be directly mixed in contexts.
   *
   * {{{
   * trait Application with Context with SecurityComponent {
   *   override lazy val modules = new SecurityModule :: new UserServiceModule :: Nil
   *   
   *   login()
   * }
   * }}}
   * 
   * @see [[sindi.Context]], [[sindi.Module]], [[sindi.ComponentContext]], [[sindi.ComponentWith]]
   */
  trait Component extends Composable

  /** A component context is an abstract class that ease the composition of components into context/module.
   *
   * {{{
   * trait Application with Context {
   *   override lazy val modules = new SecurityModule :: new UserServiceModule :: Nil
   *   val component = new ComponentContext with UserComponent with SecurityComponent
   * }
   * }}}
   *
   * @see [[sindi.Component]]
   */
  abstract class ComponentContext(implicit context: Context) extends Composable {
    protected def from[M <: Module : Manifest] = context.from[M]
  }

  /** A base trait for integrating components with global contexts.
   * 
   * By declaring a base component with a concrete [[sindi.Context]].
   *
   * {{{
   * trait ApplicationComponent extends ComponentWith[ApplicationContext.type] {
   *   val context = ApplicationContext
   * }
   * }}}
   *
   * Components can then be linked with the context and used globally.
   *
   * {{{
   * object WebBoot extends ApplicationComponent with SecurityComponent {
   *   login()
   * }
   * }}}
   */
  trait ComponentWith[C <: Context] extends Composable {
    protected val context: C
    protected def from[M <: Module : Manifest] = context.from[M]
  }

  private object Helper {
    def moduleOf[M <: Module : Manifest](module: Module): Option[M] =
      if (isModuleOf[M](module)) Some(module.asInstanceOf[M]) else None

    private def isModuleOf[M <: Module : Manifest](module: Module): Boolean =
      if (module.getClass == manifest[M].erasure) module match {
        case module: ModuleT[_] => manifest[M].typeArguments.headOption match {
          case Some(typeManifest) if module._manifest <:< manifest[M].typeArguments.head => true
          case _ => false
        }
        case _ => true
      } else false
  }

  /** This package contains exception classes */
  package exception {
    case class ModuleNotFoundException(module: Manifest[_]) extends Exception(
      "Unable to inject from module %s: module not found.".format(module))
    case class TypeNotBoundException(manifest: Manifest[_], message: String = "") extends Exception(
      ("Unable to inject %s" + message + ": type is not bound.").format(manifest))
  }
}


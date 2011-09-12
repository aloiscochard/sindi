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
package injector

object `package` {
  type Binding = Tuple2[AnyRef, binder.binding.provider.Provider[AnyRef]]
}

case class Qualifier(q: AnyRef, next: Option[Qualifier] = None) {
  def or(that: AnyRef) = Qualifier(that, Some(this))
  def ||(that: AnyRef) = or(that)
}

object Injector {
  def apply(bindings : List[Binding]): Injector =
    new DefaultInjector(bindings)
  def apply(bindings : List[Binding], parent: () => Injector): Injector = 
    new ChildedInjector(bindings, parent)
}

trait Injector {
  final def inject[T <: AnyRef : Manifest]: T = injectAs[T](Qualifier(None))
  def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T
}

private trait Bindable extends Injector {
  val bindings : List[Binding]

  override def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T = {
    def inject = injectAs[T](qualifier.q)
    qualifier.next match {
      case Some(qualifier) => try { injectAs[T](qualifier) } catch { case e: TypeNotBoundException => inject }
      case None => inject
    }
  }

  private def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    bindings.flatMap((binding) => {
      val (b_qualifier, b_provider) = binding
      if (b_qualifier == qualifier && (b_provider.signature <:< manifest[T])) {
        Some(b_provider)
      } else {
        None
      }
    }).headOption match {
      case Some(provider) => provider.provide.asInstanceOf[T]
      case None => {
        val q = if (qualifier == None) { "" } else { " with qualifier %s".format(qualifier) }
        throw TypeNotBoundException(("Unable to inject %s" + q + ": type is not bound.").format(manifest[T]))
      }
    }
  }
}

private trait Childable extends Injector {
  protected val parent: () => Injector

  override abstract def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T = {
    try {
      parent().injectAs[T](qualifier)
    } catch {
      case e: TypeNotBoundException => super.injectAs[T](qualifier)
    }
  }
}

private class DefaultInjector(
    override val bindings : List[Binding])
  extends Injector with Bindable

private class ChildedInjector(
    override val bindings : List[Binding],
    override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

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

import utils.Reflection._

object `package` {
  type Binding = Tuple2[BindingID, BindingProvider]
  type BindingProvider = () => AnyRef
  type BindingID = Tuple2[AnyRef, Manifest[_]]
}

object Injector {
  def apply(bindings : List[Binding]): Injector =
    new DefaultInjector(bindings)
  def apply(bindings : List[Binding], parent: () => Injector): Injector = 
    new ChildedInjector(bindings, parent)
}

trait Injector {
  final def inject[T <: AnyRef : Manifest]: T = injectAs[T](None)
  def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T
}

private trait Bindable extends Injector {
  val bindings : List[Binding]

  def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    bindings.flatMap((binding) => {
      val (id, provider) = binding
      if (id._1 == qualifier && isAssignable(manifest[T], id._2)) {
        Some(provider)
      } else {
        None
      }
    }).headOption match {
      case Some(provider) => provider.asInstanceOf[() => T]()
      case None => {
        val q = if (qualifier == None) { "" } else { " with qualifier %s".format(qualifier) }
        throw exception.TypeNotBoundException(("Unable to inject %s" + q + ": type is not bound.").format(manifest[T].erasure))
      }
    }
  }
}

private trait Childable extends Injector {
  val parent: () => Injector // TODO: private

  override abstract def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T = {
    try {
      parent().injectAs[T](qualifier)
    } catch {
      case e: Exception => super.injectAs[T](qualifier)
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

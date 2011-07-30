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

import scala.collection.immutable.Map

object Injector {
  def apply(bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef]): Injector =
    new DefaultInjector(bindings)
  def apply(bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef], parent: () => Injector): Injector = 
    new ChildedInjector(bindings, parent)
}

trait Injector {
  final def inject[T <: AnyRef : Manifest]: T = injectAs[T](None)
  def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T
}

private trait Bindable extends Injector {
  val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef]

  def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    val source = manifest[T].erasure
    bindings.get(qualifier -> source) match {
      case Some(provider) => provider.asInstanceOf[() => T]()
      case None => {
        val q = if (qualifier == None) { "" } else { " with qualifier %s".format(qualifier) }
        throw new RuntimeException(("Unable to inject %s" + q + ": type is not bound.").format(source))
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
    override val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef])
  extends Injector with Bindable

private class ChildedInjector(
    override val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef],
    override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

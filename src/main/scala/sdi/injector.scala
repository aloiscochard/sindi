package sdi.injector

import scala.collection.immutable.Map
import org.scalastuff.scalabeans.Preamble._

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

trait Bindable extends Injector {
  val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef]

  def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    val source = manifest[T].erasure
    bindings.get(qualifier -> source) match {
      case Some(provider) => provider.asInstanceOf[() => T]()
      case None => throw new RuntimeException("Unable to inject %s: type is not bound".format(source))
    }
  }
}

trait Annotable extends Injector {

  override abstract def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    val o = super.injectAs[T](qualifier)
    /*
    for (property <- descriptorOf(scalaTypeOf[o]).properties) {
      // inject annotated field
      println(property)
    }
    */
    o
  }
    
}

trait Childable extends Injector {
  val parent: () => Injector

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
  extends Injector with Bindable with Annotable

private class ChildedInjector(
    override val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef],
    override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

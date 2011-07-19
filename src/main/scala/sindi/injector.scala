package sindi
package injector

import scala.collection.immutable.Map
import org.scalastuff.scalabeans
import scalabeans.Preamble._
import scalabeans.MutablePropertyDescriptor

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
  class VirtualManifest(override val erasure: java.lang.Class[_]) extends Manifest[AnyRef]

  override abstract def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef) : T = {
    val o = super.injectAs[T](qualifier)
    for (p <- descriptorOf(scalaTypeOf(o.getClass)).properties) {
      p match {
        case property: MutablePropertyDescriptor => {
          property.findAnnotation[sindi.inject] match {
            case Some(annotation) => {
              val qualifier: AnyRef = if (annotation.qualifier.isEmpty) None else annotation.qualifier
              property.set(o, injectAs(qualifier)(new VirtualManifest(property.scalaType.erasure)))
            }
            case _ =>
          }
        }
        case _ =>
      }
    }
    o
  }
}

trait Childable extends Injector {
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
  extends Injector with Bindable with Annotable

private class ChildedInjector(
    override val bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef],
    override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

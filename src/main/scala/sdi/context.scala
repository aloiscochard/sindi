package sdi.context

import scala.collection.immutable.{HashMap, List, Map}

import sdi.injector.Injector
import sdi.binder.Binder

trait Context extends Injector {
  lazy val injector = factory(default)
  var bindings : Map[Tuple2[AnyRef, Class[_]], () => AnyRef] = new HashMap
  var factory : (() => Injector) => Injector = (d: () => Injector) => d() 

  protected def default : () => Injector = () => Injector(bindings)
  override def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T =  injector.injectAs[T](qualifier)
}

trait Childifiable extends Context {
  def childify(context: Context) = factory = (d) => Injector(bindings, () => context.injector)
}

trait Configurable extends Context with Binder {
  var elements: List[VirtualBinding[_]] = Nil

  def define(configure: => Unit) = {
    elements = Nil
    configure
    bindings = bindings ++ elements.map(e => e.build)
  }

  override def bind[T : Manifest] = { val e = super.bind[T]; elements = e :: elements; e }
}

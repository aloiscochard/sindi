package sindi
package context

import scala.collection.immutable.{HashMap, List, Map}

import injector.Injector

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

trait Configurable extends Context with binder.Configurable {

  def define(configure: => Unit) = {
    elements = Nil
    configure
    bindings = bindings ++ elements.map(e => e.build)
  }
}


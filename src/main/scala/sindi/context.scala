package sindi
package context

import scala.collection.immutable.{HashMap, List, Map}

import injector.Injector

trait Context extends Injector {
  lazy val injector = factory(default)

  protected val bindings: List[Tuple2[Tuple2[AnyRef, Class[_]], () => AnyRef]] = Nil

  protected var factory: (() => Injector) => Injector = (d: () => Injector) => d() 

  protected def default: () => Injector = () => Injector(bindings.toMap)

  override def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T =  injector.injectAs[T](qualifier)
}

trait Childifiable extends Context {
  def childify(context: Context) = factory = (d) => Injector(bindings.toMap, () => context.injector)
}


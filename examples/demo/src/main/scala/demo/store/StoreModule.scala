package sindi.examples.demo
package store

import sindi._

object StoreModule {
  def of[T <: AnyRef : Manifest](context: Context): StoreModule[T] = {
    implicit val c: Context = context
    new StoreModule[T]
  }
}

class StoreModule[T <: AnyRef](implicit val context: Context, val manifest: Manifest[T]) extends Module { 
  override val bindings: Bindings = bind[Store[T]] to new Store[T] with MemoryStore[T]
}

abstract class StoreComponent[T <: AnyRef : Manifest] extends Component {
  lazy val store = from[StoreModule[T]].inject[Store[T]]
}

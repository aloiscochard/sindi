package sindi.examples.demo
package store

import sindi._

class StoreModule[T <: AnyRef : Manifest](context: Context) extends ModuleT[T](context) { 
  override val bindings: Bindings = bind[Store[T]] to new Store[T] with MemoryStore[T]

  def store = inject[Store[T]]
}


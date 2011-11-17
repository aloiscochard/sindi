package sindi.examples.demo
package store

import sindi._

class StoreModule[T <: AnyRef](implicit manifest: Manifest[T], context: Context) extends ModuleT[T] { 
  override val bindings: Bindings = bind[Store[T]] to new Store[T] with MemoryStore[T]

  def store = inject[Store[T]]
}


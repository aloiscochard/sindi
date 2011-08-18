package sindi.examples.demo
package store

import sindi._

class StoreModule[T <: AnyRef : Manifest](context: Context) extends ModuleT[T](context) { 
  override val bindings = Bindings(
    bind[MemoryStore[T]] to new Store[T] with MemoryStore[T],
    bind[DiskStore[T]] to new Store[T] with DiskStore[T]
  )
}

abstract class StoreComponent[T <: AnyRef : Manifest] extends Component {
  lazy val store = from[StoreModule[T]].inject[Store[T]]
}

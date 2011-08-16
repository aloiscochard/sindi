package sindi.examples.demo
package store

trait Store[T <: AnyRef] {
  def load(entity: T): T
  def get: Option[T]
}

trait MemoryStore[T <: AnyRef] extends Store[T] {
  var entity: Option[T] = None
  override def load(e: T): T = { println("MemoryStore.load(" + e + ")"); e }
  override def get: Option[T] = entity
}

trait DiskStore[T <: AnyRef] extends Store[T] { 
  var entity: Option[T] = None
  override def load(e: T): T = { println("DiskStore.load(" + e + ")"); e }
  override def get: Option[T] = entity
}

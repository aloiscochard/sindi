package sindi.examples.demo
package store

trait Store[T <: AnyRef] {
  def load(entity: T): Option[T]
  def get: Option[T]
}

trait MemoryStore[T <: AnyRef] extends Store[T] {
  var entity: Option[T] = None
  override def load(e: T): Option[T] = { println("MemoryStore.load(" + e + ")"); entity = Some(e); entity }
  override def get: Option[T] = { println("MemoryStore.get = " + entity); entity }
}

trait DiskStore[T <: AnyRef] extends Store[T] { 
  var entity: Option[T] = None
  override def load(e: T): Option[T] = { println("DiskStore.load(" + e + ")"); entity = Some(e); entity }
  override def get: Option[T] = { println("DiskStore.get = " + entity); entity }
}

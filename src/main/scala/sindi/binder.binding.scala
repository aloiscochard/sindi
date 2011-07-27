//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi.binder
package binding

import scala.collection.mutable.{HashMap => MHashMap}

import scala.ref.WeakReference

object Binding {
  def apply[T <: AnyRef : Manifest](provider: () => T): Binding[T] =
    new DefaultBinding[T](manifest[T].erasure.asInstanceOf[Class[T]], provider)
  def apply[T <: AnyRef](binding: Binding[T], scoper: () => Any): Binding[T] =
    new ScopedBinding[T](binding, scoper)
  def apply[T <: AnyRef](binding: Binding[T], qualifier: AnyRef): Binding[T] =
    new QualifiedBinding[T](binding, qualifier)
}

trait Binding[T <: AnyRef] {
  protected[binding] val source: Class[T]

  protected val provider: () => T

  def build: Tuple2[Tuple2[AnyRef,Class[T]], () => T] = (None, source) -> provider
}

trait Scopable[T <: AnyRef] extends Binding[T] {
  // TODO [aloiscochard] Check if object are GCed correctly using WeakReference
  protected val registry = new MHashMap[Int, WeakReference[T]] // WARNING: mutable datastructure
  protected val scoper: () => Any

  override def build = {
    val e = super.build
    e._1 -> (() => { registry.getOrElseUpdate(scoper().hashCode, new WeakReference[T](e._2())).apply })
  }
}

trait Qualifiable[T <: AnyRef] extends Binding[T] {
  protected val qualifier: AnyRef

  override def build = {
    val e = super.build
    (qualifier -> e._1._2) -> e._2
  }
}

private class DefaultBinding[T <: AnyRef](val source: Class[T], val provider: () => T)
  extends Binding[T]

private class ScopedBinding[T <: AnyRef](binding: Binding[T], val scoper: () => Any)
  extends DefaultBinding[T](binding.source, binding.build._2) with Scopable[T] 

private class QualifiedBinding[T <: AnyRef](binding: Binding[T], val qualifier: AnyRef)
  extends DefaultBinding[T](binding.source, binding.build._2) with Qualifiable[T]

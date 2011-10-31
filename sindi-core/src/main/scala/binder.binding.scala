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

import sindi.binder.binding.provider.{Provider, FunctionProvider}

trait Binding[T <: AnyRef] {
  protected val provider: Provider[T]

  def build: Tuple2[AnyRef, Provider[T]] = (None, provider)
}

protected[binder] object Binding {
  def apply[T <: AnyRef : Manifest](provider: Provider[T]): Binding[T] = new DefaultBinding[T](provider)
  def apply[T <: AnyRef](binding: Binding[T], scoper: () => Any): Binding[T] = new ScopedBinding[T](binding, scoper)
  def apply[T <: AnyRef](binding: Binding[T], qualifier: AnyRef): Binding[T] = new QualifiedBinding[T](binding, qualifier)
}

private trait Scopable[T <: AnyRef] extends Binding[T] {
  // TODO [aloiscochard] Check if object are GCed correctly using WeakReference
  protected val registry = new MHashMap[Int, WeakReference[T]] // WARNING: mutable datastructure
  protected val scoper: () => Any

  override def build = super.build match {
    case (qualifier, provider) => (qualifier, new FunctionProvider[T](provider.signature, () =>
        registry.getOrElseUpdate(scoper().hashCode, new WeakReference[T](provider())).apply
      )
    )
  }
}

private trait Qualifiable[T <: AnyRef] extends Binding[T] {
  protected val qualifier: AnyRef
  override def build = qualifier -> super.build._2
}

private class DefaultBinding[T <: AnyRef](val provider: Provider[T])
  extends Binding[T]

private class ScopedBinding[T <: AnyRef](binding: Binding[T], val scoper: () => Any)
  extends DefaultBinding[T](binding.build._2) with Scopable[T] 

private class QualifiedBinding[T <: AnyRef](binding: Binding[T], val qualifier: AnyRef)
  extends DefaultBinding[T](binding.build._2) with Qualifiable[T]

package provider {
  trait Provider[T <: AnyRef] {
    val signature: Manifest[T]
    def apply(): T
  }

  class FunctionProvider[T <: AnyRef](override val signature: Manifest[T], val f: () => T) extends Provider[T] {
    def apply() = f()
  }
}

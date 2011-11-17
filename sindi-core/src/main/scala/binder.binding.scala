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

import sindi.provider.Provider

trait Binding[T <: AnyRef] {
  protected val provider: Provider[T]

  def build: Tuple2[Provider[T], AnyRef] = (provider, None)
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
    case (provider, qualifier) => (Provider { 
      registry.getOrElseUpdate(scoper().hashCode, new WeakReference[T](provider())).apply
    }(provider.signature), qualifier)
    
  }
}

private trait Qualifiable[T <: AnyRef] extends Binding[T] {
  protected val qualifier: AnyRef
  override def build = super.build._1 -> qualifier
}

private class DefaultBinding[T <: AnyRef](val provider: Provider[T])
  extends Binding[T]

private class ScopedBinding[T <: AnyRef](binding: Binding[T], val scoper: () => Any)
  extends DefaultBinding[T](binding.build._1) with Scopable[T] 

private class QualifiedBinding[T <: AnyRef](binding: Binding[T], val qualifier: AnyRef)
  extends DefaultBinding[T](binding.build._1) with Qualifiable[T]

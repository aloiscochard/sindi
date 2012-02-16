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

trait Binding[T] {
  protected val provider: Provider[T]

  def build: Tuple2[Provider[T], Any] = (provider, None)
}

protected[binder] object Binding {
  def apply[T : Manifest](provider: Provider[T]): Binding[T] = new DefaultBinding[T](provider)
  def apply[T](binding: Binding[T], scoper: () => Any): Binding[T] = new ScopedBinding[T](binding, scoper)
  def apply[T](binding: Binding[T], qualifier: Any): Binding[T] = new QualifiedBinding[T](binding, qualifier)
}

private trait Scopable[T] extends Binding[T] {
  // TODO [aloiscochard] Check if object are GCed correctly using WeakReference
  //protected val registry = new MHashMap[Int, WeakReference[T]] // WARNING: mutable datastructure
  protected val registry = new MHashMap[Int, T] // WARNING: mutable datastructure
  protected val scoper: () => Any

  override def build = super.build match {
    case (provider, qualifier) => (Provider.create( 
      //registry.getOrElseUpdate(scoper().hashCode, new WeakReference[T](provider())).apply,
      registry.getOrElseUpdate(scoper().hashCode, provider()),
      provider.signature),
    qualifier)
  }
}

private trait Qualifiable[T] extends Binding[T] {
  protected val qualifier: Any
  override def build = super.build._1 -> qualifier
}

private class DefaultBinding[T](val provider: Provider[T])
  extends Binding[T]

private class ScopedBinding[T](binding: Binding[T], val scoper: () => Any)
  extends DefaultBinding[T](binding.build._1) with Scopable[T] 

private class QualifiedBinding[T](binding: Binding[T], val qualifier: Any)
  extends DefaultBinding[T](binding.build._1) with Qualifiable[T]

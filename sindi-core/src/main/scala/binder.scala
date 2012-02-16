//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi
package binder

import binder.binding._
import provider.Provider

trait Binder extends Scoper {
  protected def bind[T : Manifest](provider: Provider[T]): Binding[T] = Binding[T](provider)
  protected def scopify[T](binding: Binding[T])(scoper: () => Any): Binding[T] = Binding[T](binding, scoper)
  protected def qualify[T](binding: Binding[T], qualifier: Any): Binding[T] = Binding[T](binding, qualifier)
}

trait Scoper {
  /** A current thread based scoper. */
  def thread: () => Any = java.lang.Thread.currentThread _
}

trait DSL {
  /** Start a new binding declaration. */
  def bind[T : Manifest] = new BindSource[T]
  /** Return a provider with the specified function **/
  def provider[T: Manifest](injection: => T) = Provider(injection)

  protected class BindSource[T : Manifest] extends Binder {
    /** Assign given injection to this binding declaration as cached provider. */
    def to(injection: => T): SimpleBind[T] = to(Provider.cached(injection))
    /** Assign given provider to this binding declaration. */
    def to[P <: T : Manifest](provider: Provider[P]): SimpleBind[T] =
      new SimpleBind[T](provider)//.asInstanceOf[SimpleBind[T]]
  }

  protected sealed abstract class Bind[T : Manifest] extends Binder {
    /** Return a new binding from this binding declaration. */
    def build: Binding[T]
    protected def toQualified(qualifier: Any): QualifiedBind[T] = new QualifiedBind[T](this, qualifier)
    protected def toScopable(scoper: () => Any): ScopedBind[T] = new ScopedBind[T](this, scoper)
  }

  protected trait Qualifiable[T] extends Bind[T] {
    /** Assign given qualifer to this binding declaration. */
    def as(qualifier: Any) = toQualified(qualifier)
    /** Assign given type as qualifer to this binding declaration. */
    def as[Q : Manifest] = toQualified(manifest[Q])
  }

  protected trait Scopable[T] extends Bind[T] {
    /** Assign given scoper to this binding declaration. */
    def scope(scoper: => Any) = toScopable(scoper _)
  }
  
  protected class SimpleBind[T : Manifest](provider: Provider[T]) extends Bind[T] with Qualifiable[T] with Scopable[T] {
    override def build = bind(provider)
  }

  protected class ScopedBind[T : Manifest](bind: Bind[T], scoper: () => Any) extends Bind[T] {
    override def build = scopify(bind.build)(scoper)
  }

  protected class QualifiedBind[T : Manifest](bind: Bind[T], qualifier: Any) extends Bind[T] with Scopable[T] {
    override def build = qualify(bind.build, qualifier)
  }

  protected implicit def bind2binding[T : Manifest](bind: Bind[T]): Binding[T] = bind.build
  protected implicit def bind2bindings[T : Manifest](bind: Bind[T]): Bindings =
    List[binding.Binding[Any]](bind.build.asInstanceOf[binding.Binding[Any]])
}

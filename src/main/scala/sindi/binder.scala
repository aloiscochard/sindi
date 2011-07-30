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

trait Scoper {
  def thread: () => AnyRef = () => java.lang.Thread.currentThread
}

trait Binder extends Scoper {
  def bind[T <: AnyRef : Manifest](provider: => T): Binding[T] = Binding[T](() => provider)
  def scopify[T <: AnyRef](binding: Binding[T])(scoper: () => Any): Binding[T] = Binding[T](binding, scoper)
  def qualify[T <: AnyRef](binding: Binding[T], qualifier: AnyRef): Binding[T] = Binding[T](binding, qualifier)
}

trait DSL {
  type Bindings = List[binding.Binding[_]]
  object Bindings { def apply(bindings: binding.Binding[_]*): List[binding.Binding[_]] = bindings.toList }

  def bind[T <: AnyRef : Manifest] = new BindSource[T]

  class BindSource[T <: AnyRef : Manifest] extends Binder {
    def to(provider: => T) = new SimpleBind[T](provider)
  }

  abstract class Bind[T <: AnyRef : Manifest] extends Binder {
    def build: Binding[T]
    protected def toQualified(qualifier: AnyRef): QualifiedBind[T] = new QualifiedBind[T](this, qualifier)
    protected def toScopable(scoper: () => Any): ScopedBind[T] = new ScopedBind[T](this, scoper)
  }

  trait Qualifiable[T <: AnyRef] extends Bind[T] { def as(qualifier: AnyRef) = toQualified(qualifier) }
  trait Scopable[T <: AnyRef] extends Bind[T] { def scope(scoper: => Any) = toScopable(() => scoper) }
  
  class SimpleBind[T <: AnyRef : Manifest](provider: => T) extends Bind[T] with Qualifiable[T] with Scopable[T] {
    override def build = bind(provider)
  }

  class ScopedBind[T <: AnyRef : Manifest](bind: Bind[T], scoper: () => Any) extends Bind[T] {
    override def build = scopify(bind.build)(scoper)
  }

  class QualifiedBind[T <: AnyRef : Manifest](bind: Bind[T], qualifier: AnyRef) extends Bind[T] with Scopable[T] {
    override def build = qualify(bind.build, qualifier)
  }

  implicit def bind2binding[T <: AnyRef : Manifest](bind: Bind[T]): Binding[T] = bind.build
  implicit def bind2bindings[T <: AnyRef : Manifest](bind: Bind[T]): List[Binding[_]] = List(bind.build)
}

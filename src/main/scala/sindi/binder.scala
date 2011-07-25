package sindi
package binder

import binder.binding._

trait Scoper {
  def singleton: () => AnyRef = () => None // TODO Move singleton concept to DSL, to avoide mutable state of scopable.
  def thread: () => AnyRef = () => java.lang.Thread.currentThread
}

trait Binder extends Scoper {
  def bind[T <: AnyRef : Manifest](provider: => T): Binding[T] = Binding[T](() => provider)
  def scopify[T <: AnyRef](binding: Binding[T])(scoper: () => Any): Binding[T] = Binding[T](binding, scoper)
  def qualify[T <: AnyRef](binding: Binding[T], qualifier: AnyRef): Binding[T] = Binding[T](binding, qualifier)

  /////////
  // DSL //
  /////////

  def bind[T <: AnyRef : Manifest] = new VirtualBinding[T]

  protected class VirtualBinding[T <: AnyRef : Manifest] (
      var _provider: () => T = null,
      var _qualifier: AnyRef = None,
      var _scoper: () => Any = null) extends Binding[T] {

    override val source = null
    override val provider = null

    val _source : Class[T] = manifest[T].erasure.asInstanceOf[Class[T]]

    def to(provider: => T) = { _provider = () => provider; this }
    def scope(scoper: () => Any) = { _scoper = scoper; this }
    def as(qualifier: AnyRef) = { _qualifier = qualifier; this }

    override def build = {
      assert(_source != null); assert(_provider != null)
      var binding = Binding(_provider.asInstanceOf[() => T])
      if (_qualifier != null) binding = qualify(binding, _qualifier)
      if (_scoper != null) binding = scopify(binding)(_scoper)
      binding.build
    }
  }

}

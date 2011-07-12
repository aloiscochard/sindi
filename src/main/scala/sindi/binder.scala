package sindi
package binder

import binder.binding._

object Binder extends Binder

trait Scoper {
  def singleton: () => AnyRef = () => None // TODO Move singleton concept to DSL, to avoide mutable state of scopable.
  def thread: () => AnyRef = () => java.lang.Thread.currentThread
}

trait Binder extends Scoper {

  def bind[T <: AnyRef : Manifest](provider: => T) : Binding = Binding(manifest[T].erasure, () => provider)
  def scopify(binding: Binding)(scoper: () => Any) : Binding = Binding(binding, scoper)
  def qualify(binding: Binding, qualifier: AnyRef) : Binding = Binding(binding, qualifier)

  /////////
  // DSL //
  /////////

  def bind[T : Manifest] = new VirtualBinding[T]

  implicit def binding2tuple(b: Binding): Tuple2[Tuple2[AnyRef, Class[_]], () => AnyRef] = { b.build }

  protected class VirtualBinding[T : Manifest](
      var _provider: () => T = null,
      var _qualifier: AnyRef = None,
      var _scoper: () => Any = null) extends Binding{

    override val source = null
    override val provider = null

    val _source : Class[_] = manifest[T].erasure

    def to(provider: => T) = { _provider = () => provider; this }
    def scope(scoper: () => Any) = { _scoper = scoper; this }
    def as(qualifier: AnyRef) = { _qualifier = qualifier; this }

    override def build = {
      assert(_source != null); assert(_provider != null)
      var binding = Binding(_source, _provider.asInstanceOf[() => AnyRef])
      if (_qualifier != null) binding = qualify(binding, _qualifier)
      if (_scoper != null) binding = scopify(binding)(_scoper)
      binding.build
    }
  }

}

trait Configurable extends Binder {
  protected var elements: List[VirtualBinding[_]] = Nil

  override def bind[T : Manifest] = { val e = super.bind[T]; elements = e :: elements; e }
}

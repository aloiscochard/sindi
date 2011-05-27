package sindi.binder.binding

import scala.collection.mutable.{HashMap => MHashMap}

import scala.ref.WeakReference

object Binding {
  def apply(source: Class[_], provider: () => AnyRef): Binding = new DefaultBinding(source, provider)
  def apply(binding: Binding, scoper: () => Any): Binding = new ScopedBinding(binding, scoper)
  def apply(binding: Binding, qualifier: AnyRef): Binding = new QualifiedBinding(binding, qualifier)
}

trait Binding {
  val source: Class[_]
  val provider: () => AnyRef

  def build: Tuple2[Tuple2[AnyRef,Class[_]], () => AnyRef] = (None, source) -> provider
}

trait Scopable extends Binding {
  // TODO [aloiscochard] Check if object are GCed correctly using WeakReference
  protected val registry = new MHashMap[Int, WeakReference[AnyRef]]

  val scoper: () => Any

  override def build = {
    val e = super.build
    e._1 -> (() => { registry.getOrElseUpdate(scoper().hashCode, new WeakReference(e._2())).apply })
  }
}

trait Qualifiable extends Binding {
  val qualifier: AnyRef

  override def build = {
    val e = super.build
    (qualifier -> e._1._2) -> e._2
  }
}

private class DefaultBinding(val source: Class[_], val provider: () => AnyRef)
  extends Binding

private class ScopedBinding(binding: Binding, val scoper: () => Any)
  extends DefaultBinding(binding.source, binding.build._2) with Scopable 

private class QualifiedBinding(binding: Binding, val qualifier: AnyRef)
  extends DefaultBinding(binding.source, binding.build._2) with Qualifiable

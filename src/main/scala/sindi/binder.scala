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

  /////////
  // DSL //
  /////////

  def bind[T <: AnyRef : Manifest] = VirtualBinding[T]()

  protected case class VirtualBinding[T <: AnyRef : Manifest] (
      val _provider: Option[() => T] = None,
      val _qualifier: Option[AnyRef] = None,
      val _scoper: Option[() => Any] = None) extends Binding[T] {

    override val provider = null

    val source: Class[T] = manifest[T].erasure.asInstanceOf[Class[T]]

    def to(provider: => T) = { VirtualBinding[T](Some(() => provider), _qualifier, _scoper) }
    def as(qualifier: AnyRef) = { VirtualBinding[T](_provider, Some(qualifier), _scoper) }
    def scope(scoper: () => Any) = { VirtualBinding[T](_provider, _qualifier, Some(scoper)) }

    override def build = {
      assert(source != null); assert(_provider != null)
      if (_qualifier.isEmpty && _scoper.isEmpty) {
        Binding(_provider.get).build
      } else if (!_qualifier.isEmpty && _scoper.isEmpty) {
        qualify(Binding(_provider.get), _qualifier.get).build
      } else if (_qualifier.isEmpty && !_scoper.isEmpty) {
        scopify(Binding(_provider.get))(_scoper.get).build
      } else {
        scopify(qualify(Binding(_provider.get), _qualifier.get))(_scoper.get).build
      }
    }
  }

}

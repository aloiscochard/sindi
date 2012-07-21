//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2012, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi
package context

abstract class ContextSindi[Q]()(implicit mQ: Manifest[Q]) { 
  implicit def list2wire[T : Manifest] = new Wire(injectAll[T])

  def :<:[T : Manifest](x: => T) = bind(x)
  def <<[T : Manifest](x: => T) = bind(x)
  def ++>[T : Manifest] = injectAll[T]

  def bind[T : Manifest](x: => T) = register(Binding[T, Q](x))
  def injectAll[T : Manifest]: Seq[T]

  protected def register[T, Q](binding: Binding[T, Q])(implicit mT: Manifest[T], mQ: Manifest[Q]): Binding[T, Q]
}

abstract class ContextQualifier[Q](implicit mQ: Manifest[Q]) extends ContextSindi[Q] with Qualifier[Q]

trait Support {
  class Context extends ContextSindi[Default]()(manifest[Default]) {
    def as[Q](implicit qualifier: Qualifier[Q], manifestQ: Manifest[Q]) = new ContextQualifier[Q] {
      override def injectAll[T : Manifest]: Seq[T] =
        Context.this._filterAndInject[T] { case (mT, mQ, b) => mT <:< manifest[T] && mQ <:< manifestQ }

      override protected def register[T, Q](binding: Binding[T, Q])(implicit mT: Manifest[T], mQ: Manifest[Q]) =
        Context.this.register(binding)
    }

    override def injectAll[T : Manifest]: Seq[T] =
      _filterAndInject { case (mT, mQ, b) => mT <:< manifest[T] }

    override protected def register[T, Q](binding: Binding[T, Q])(implicit mT: Manifest[T], mQ: Manifest[Q]): Binding[T, Q] = {
      registry = (manifest[T], manifest[Q], binding) :: registry
      binding
    }

    private def _filterAndInject[T](f: ((Manifest[_], Manifest[_], Binding[_, _])) => Boolean): Seq[T] =
      registry.filter(f).toList.view.map(_._3.inject.asInstanceOf[T])

    protected var registry = List[(Manifest[_], Manifest[_], Binding[_, _])]()
  }

  object Context { def apply() = new Context }
}


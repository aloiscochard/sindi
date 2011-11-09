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
package injector

import scala.Stream._
import scala.util.control.Exception._

object `package` {
  type Binding = Tuple2[AnyRef, binder.binding.provider.Provider[AnyRef]]
}

case class Qualifier(q: AnyRef, next: Option[Qualifier] = None) {
  def or(that: AnyRef) = Qualifier(that, Some(this))
  def ||(that: AnyRef) = or(that)
}

object Injector {
  def apply(bindings : List[Binding]): Injector =
    new DefaultInjector(bindings)
  def apply(bindings : List[Binding], parent: () => Injector): Injector = 
    new ChildedInjector(bindings, parent)
}

trait Injector {
  final def inject[T <: AnyRef : Manifest]: T = injectAs[T](Qualifier(None))
  final def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T = injection[T](qualifier).apply
  final def injectAll[T <: AnyRef : Manifest]: Stream[T] = injectAll[T] { x: Qualifier => true }
  final def injectAll[T <: AnyRef : Manifest](qualifier: Qualifier): Stream[T] = injectAll[T] { x: Qualifier => x == qualifier }
  final def injectAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[T] = injectionAll[T](predicate).map(_.apply)
  def injection[T <: AnyRef : Manifest](qualifier: Qualifier): () => T
  def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[() => T]
}

private trait Bindable extends Injector {
  val bindings : List[Binding]

  override def injection[T <: AnyRef : Manifest](qualifier: Qualifier) = () =>
    qualifier.next.flatMap(qualifier => catching(classOf[TypeNotBoundException]).opt(injectAs[T](qualifier))).getOrElse {
      bindings.view.filter(isBound(_)(manifest[T])(qualifier))
        .map { case (q, p) => p().asInstanceOf[T] }
        .headOption.getOrElse {
          val q = if (qualifier == None) { "" } else { " with qualifier %s".format(qualifier) }
          throw TypeNotBoundException(("Unable to inject %s" + q + ": type is not bound.").format(manifest[T]))
        }
    }

  override def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[() => T] = bindings.toStream
    .filter(isBound(manifest[T]) _).filter { case (q, _) => predicate(q) }
    .map { case (_, p) => () => p().asInstanceOf[T] }

  private def isBound(manifest: Manifest[_])(b: Binding): Boolean = b._2.signature <:< manifest
  private def isBound(qualifier: Qualifier)(b: Binding): Boolean = b._1 == qualifier.q
  private def isBound(b: Binding): Manifest[_] => Qualifier => Boolean = {
    // TODO Find a generalization for this (look at |+| on Arrows in scalaz)
    def f[A, B, C](ac: A => C)(bc: B => C)(ccc: (C, C) => C)(a: A)(b: B): C = ccc(ac(a), bc(b))
    f(isBound(_: Manifest[_])(b))(isBound(_: Qualifier)(b))(_ && _) _
  }
}

private trait Childable extends Injector {
  protected val parent: () => Injector

  abstract override def injection[T <: AnyRef : Manifest](qualifier: Qualifier) =
    catching(classOf[TypeNotBoundException]).opt(parent().injection[T](qualifier)).getOrElse(super.injection[T](qualifier))

  abstract override def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[() => T] =
    super.injectionAll[T](predicate).append(parent().injectionAll[T](predicate))
}

private class DefaultInjector(override val bindings : List[Binding])
  extends Injector with Bindable

private class ChildedInjector(override val bindings : List[Binding], override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

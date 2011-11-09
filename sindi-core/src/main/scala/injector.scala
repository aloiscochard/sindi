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

/** An interface containing operations for object injection. */
trait Injector {
  /** Return the object associated with a given type. */
  final def inject[T <: AnyRef : Manifest]: T =
    injection[T].apply
  /** Return the object associated with a given type and qualifier. */
  final def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T =
    injectionAs[T](qualifier).apply
  /** Return all objects associated with a given type. */
  final def injectAll[T <: AnyRef : Manifest]: Stream[T] =
    injectionAll[T].map(_.apply)
  /** Return all objects associated with a given type and qualifier. */
  final def injectAll[T <: AnyRef : Manifest](qualifier: Qualifier): Stream[T] =
    injectionAll[T](qualifier).map(_.apply)
  /** Return all objects associated with a given type and that satisfy a given predicate. */
  final def injectAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[T] =
    injectionAll[T](predicate).map(_.apply)

  /** Return the injection associated with a given type. */
  final def injection[T <: AnyRef : Manifest]: () => T =
    injectionAs[T](Qualifier(None))
  /** Return all injections associated with a given type. */
  final def injectionAll[T <: AnyRef : Manifest]: Stream[() => T] =
    injectionAll[T] { x: Qualifier => true }
  /** Return all injections associated with a given type and qualifier. */
  final def injectionAll[T <: AnyRef : Manifest](qualifier: Qualifier): Stream[() => T] =
    injectionAll[T] { x: Qualifier => x == qualifier }

  /** Return the injection associated with a given type and qualifier. */
  def injectionAs[T <: AnyRef : Manifest](qualifier: Qualifier): () => T
  /** Return all injections associated with a given type and that satisfy a given predicate. */
  def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[() => T]
}

private trait Bindable extends Injector {
  val bindings : List[Binding]

  override def injectionAs[T <: AnyRef : Manifest](qualifier: Qualifier) = () =>
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

  abstract override def injectionAs[T <: AnyRef : Manifest](qualifier: Qualifier) =
    catching(classOf[TypeNotBoundException]).opt(parent().injectionAs[T](qualifier)).getOrElse(super.injectionAs[T](qualifier))

  abstract override def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean): Stream[() => T] =
    super.injectionAll[T](predicate).append(parent().injectionAll[T](predicate))
}

private class DefaultInjector(override val bindings : List[Binding])
  extends Injector with Bindable

private class ChildedInjector(override val bindings : List[Binding], override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

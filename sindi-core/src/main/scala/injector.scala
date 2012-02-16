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

import exception._
import provider._

object `package` {
  type Binding = Tuple2[Provider[Any], Any]
  type Bindings = Seq[Tuple2[Provider[Any], Any]]
  type Injection[T] = () => T
}

/** A sequence of qualifier. */
case class Qualifiers(current: Any, next: Option[Qualifiers] = None) {
  def or(that: Any) = Qualifiers(that, Some(this))
  def ||(that: Any) = or(that)

  def toList: List[Any] = next match { case Some(next) => current :: next.toList; case None => current :: Nil }

  override def toString = toList.mkString("(", " || ", ")")
}

object Injector {
  def apply(bindings : Bindings): Injector =
    new DefaultInjector(bindings)
  def apply(bindings : Bindings, parent: () => Injector): Injector = 
    new ChildedInjector(bindings, parent)
}

/** An interface containing operations for object injection. */
trait Injector {
  /** Return the object associated with a given type. */
  final def inject[T : Manifest]: T =
    injection[T].apply
  /** Return the object associated with a given type and qualifiers. */
  final def injectAs[T : Manifest](qualifiers: Qualifiers): T =
    injectionAs[T](qualifiers).apply
  /** Return all objects associated with a given type. */
  final def injectAll[T : Manifest]: Stream[T] =
    injectionAll[T].map(_.apply)
  /** Return all objects associated with a given type and qualifiers. */
  final def injectAll[T : Manifest](qualifiers: Qualifiers): Stream[T] =
    injectionAll[T](qualifiers).map(_.apply)

  /** Return the injection associated with a given type. */
  final def injection[T : Manifest]: Injection[T] =
    injectionAs[T](Qualifiers(None))
  /** Return all injections associated with a given type. */
  final def injectionAll[T : Manifest]: Stream[Injection[T]] =
    injectionAll[T](Qualifiers(None))

  /** Return the injection associated with a given type and qualifiers. */
  def injectionAs[T : Manifest](qualifiers: Qualifiers): Injection[T]
  /** Return all injections associated with a given type and qualifiers. */
  def injectionAll[T : Manifest](qualifiers: Qualifiers): Stream[Injection[T]]
}

private trait Bindable extends Injector {
  protected val bindings: Bindings

  override def injectionAs[T : Manifest](qualifiers: Qualifiers) = () =>
    qualifiers.next.flatMap(qualifiers => catching(classOf[TypeNotBoundException]).opt(injectAs[T](qualifiers))).getOrElse {
      bindings.view.filter(isBound(_)(manifest[T])(qualifiers.current))
        .map { case (p, _) => p().asInstanceOf[T] }
        .headOption.getOrElse {
          throw TypeNotBoundException(manifest[T], qualifiers match {
            case Qualifiers(None, None) => ""
            case qualifiers => " with qualifiers %s".format(qualifiers)
          })
        }
    }

  override def injectionAll[T : Manifest](qualifiers: Qualifiers) = bindings.toStream
    .filter(isBound(_)(manifest[T])(qualifiers))
    .map { case (p, _) => () => p().asInstanceOf[T] }

  private def isBound(manifest: Manifest[_])(b: Binding): Boolean = b._1.signature <:< manifest

  private def isBound(qualifiers: Qualifiers)(b: Binding): Boolean = qualifiers.next match {
    case Some(q) => isBound(q)(b) || b._2 == qualifiers.current
    case None => b._2 == qualifiers.current
  }

  private def isBound(b: Binding): Manifest[_] => Qualifiers => Boolean = {
    // TODO Find a generalization for this (look at |+| on Arrows in scalaz)
    def f[A, B, C](ac: A => C)(bc: B => C)(ccc: (C, C) => C)(a: A)(b: B): C = ccc(ac(a), bc(b))
    f(isBound(_: Manifest[_])(b))(isBound(_: Qualifiers)(b))(_ && _) _
  }
}

private trait Childable extends Injector {
  protected val parent: () => Injector

  abstract override def injectionAs[T : Manifest](qualifiers: Qualifiers) = () =>
    catching(classOf[TypeNotBoundException]).opt(parent().injectAs[T](qualifiers))
      .getOrElse(super.injectionAs[T](qualifiers).apply)

    abstract override def injectionAll[T : Manifest](qualifiers: Qualifiers) =
    super.injectionAll[T](qualifiers).append(parent().injectionAll[T](qualifiers))
}

private class DefaultInjector(override protected val bindings : Bindings)
  extends Injector with Bindable

private class ChildedInjector(override protected val bindings : Bindings, override val parent: () => Injector)
  extends DefaultInjector(bindings) with Childable

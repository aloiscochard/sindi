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

// TODO Add operators (in syntax specific module)

trait Binding[T, Q] { def inject: T }

object Binding { def apply[T, Q](x: => T) = new Binding[T, Q] { def inject = x } }

trait BindingToEither extends BindingToRight

trait BindingToRight extends BindingToLeft {
  implicit def bindingEitherR0[T, Q0, Q1](implicit b1: Binding[T, Q1]): Either[Binding[T, Q0], Binding[T, Q1]] = Right(b1)
  implicit def bindingEitherR1[T0, Q0, T1, Q1](implicit b1: Binding[T1, Q1]): Either[Binding[T0, Q0], Binding[T1, Q1]] = Right(b1)
}

trait BindingToLeft {
  implicit def bindingEitherL0[T, Q0, Q1](implicit b0: Binding[T, Q0]): Either[Binding[T, Q0], Binding[T, Q1]] = Left(b0)
  implicit def bindingEitherL1[T0, Q0, T1, Q1](implicit b0: Binding[T0, Q0]): Either[Binding[T0, Q0], Binding[T1, Q1]] = Left(b0)
}

trait Default

trait Qualifier[Q]

class QualifiersOps[Q0, Q1](tuple: (Qualifier[Q0], Qualifier[Q1])) { 
  def inject[T](implicit either: Either[Binding[T, Q1], Binding[T, Q0]]) = either match {
    case Right(binding) => binding.inject
    case Left(binding) => binding.inject
  }
  def injectOption[T](implicit o0: Option[Binding[T, Q0]] = None, o1: Option[Binding[T, Q1]] = None) = o0.orElse(o1).map(_.inject)
}

trait Sindi[Q] extends Wiring[Q] {
  def bind[T](x: => T) = Binding[T, Q](x)
  def inject[T](implicit binding: Binding[T, Q]) = binding.inject

  def injectEither[T0, T1](implicit either: Either[Binding[T0, Q], Binding[T1, Q]]): Either[T0, T1] = either match {
    case Right(binding) => Right(binding.inject)
    case Left(binding) => Left(binding.inject)
  }

  def injectOption[T](implicit option: Option[Binding[T, Q]] = None) = option.map(_.inject)
}

class Wire[T](value: => T) { def apply() = value }

trait Wiring[Q] { self: Sindi[Q] =>
  implicit def any2wire[T](implicit x: T) = new Wire(x) // TODO TEST THIS! (override binding using implicit of T)

  implicit def binding2wire[T](implicit binding: Binding[T, Q]) =
    new Wire(inject[T])
  implicit def bindingOption2wire[T](implicit option: Option[Binding[T, Q]] = None) =
    new Wire(injectOption[T])
  implicit def bindingEither2wire[T0, T1](implicit binding: Either[Binding[T0, Q], Binding[T1, Q]]) = 
    new Wire(injectEither[T0, T1])

  def autowire[A, B](f: (A) => B)(implicit wire: Wire[A]): B = f(wire())
  def wire[T](implicit wire: Wire[T]) = wire()
}

object core extends Sindi[Default] with BindingToEither {

  type Default = sindi.Default

  implicit def bindingOption[T, Q](implicit binding: Binding[T, Q]) = Some(binding)
  implicit def injection[T](binding: Binding[T, _]): T = binding.inject
  implicit def qualifier[Q] = new Qualifier[Q] {}
  implicit def qualifierOps[Q](qualifer: Qualifier[Q]) = new Sindi[Q] {}
  implicit def qualifiersOps[Q0, Q1](tuple: (Qualifier[Q0], Qualifier[Q1])) = new QualifiersOps(tuple)

  def as[T](implicit qualifier: Qualifier[T]) = qualifier
  def bind[T, Q](x: => T, qualifier: Qualifier[Q]) = Binding[T, Q](x)
}


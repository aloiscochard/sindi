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
package syntax

trait Sindi[Q] { self: sindi.Sindi[Q] =>
  def :<:[T](x: => T) = bind(x)
  def :+:[T](x: => T) = provide(x)
  def <<[T](x: => T) = bind(x)
  def <+[T](x: => T) = provide(x)
  def +>[T](implicit binding: Binding[T, Q]) = inject[T]
}

trait Wiring[Q] { self: sindi.Wiring[Q] =>
  def :>:[T : Wire] = wire[T]
  def >>[T : Wire] = wire[T]
  def >>>[A, B](f: (A) => B)(implicit wire: Wire[A]): B = autowire(f)
}


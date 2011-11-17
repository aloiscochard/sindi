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
package processor

import scala.util.control.Exception._

import exception._
import injector.{Injector, Injection, Qualifiers}

object `package` {
  val option = new OptionProcessor
  val either = new EitherProcessor
}

object Processor {
  def process[T : Manifest](processors: List[Processor[_]], injector: Injector, qualifiers: Qualifiers, injection: Injection[T]) =
    processors.foldLeft(injection)((injection, processor) =>
      if (manifest[T] <:< processor.scope)
        () => processor.asInstanceOf[Processor[T]].process[T](injector, qualifiers, injection)
      else injection
    )
}

abstract class Processor[T : Manifest] {
  def scope = manifest[T]
  def process[P <: T : Manifest](injector: Injector, qualifiers: Qualifiers, injection: Injection[P]): P
}

private[processor] class OptionProcessor extends Processor[Option[Any]] {
  def process[T <: Option[_] : Manifest](injector: Injector, qualifiers: Qualifiers, injection: Injection[T]) =
    manifest[T].typeArguments.headOption match {
      case Some(manifest) => catching(classOf[TypeNotBoundException]).opt {
          Some(injector.injectAs(qualifiers)(manifest.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse None.asInstanceOf[T]
      case None => injection()
    }
}

private[processor] class EitherProcessor extends Processor[Either[Any, Any]] {
  def process[T <: Either[_, _] : Manifest](injector: Injector, qualifiers: Qualifiers, injection: Injection[T]) =
    manifest[T].typeArguments match {
      case left :: right :: Nil => catching(classOf[TypeNotBoundException]).opt {
          Right(injector.injectAs(qualifiers)(right.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse(catching(classOf[TypeNotBoundException]).opt {
          Left(injector.injectAs(qualifiers)(left.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse injection())
      case _ => injection()
    }
}

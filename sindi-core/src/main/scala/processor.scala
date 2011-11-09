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

import injector.{Injector, Qualifier}

object `package` {
  val option = new OptionProcessor
  val either = new EitherProcessor
}

object Processor {
  def process[T : Manifest](processors: List[Processor[_]], injector: Injector, qualifier: Qualifier, default: () => T) =
    processors.foldLeft(default)((default, processor) => {
      if (manifest[T] <:< processor.scope)
        () => processor.asInstanceOf[Processor[T]].process[T](injector, qualifier, default)
      else default
    })
}

abstract class Processor[T : Manifest] {
  def scope = manifest[T]
  def process[P <: T : Manifest](injector: Injector, qualifier: Qualifier, default: () => P): P
}

private[processor] class OptionProcessor extends Processor[Option[Any]] {
  def process[T <: Option[_] : Manifest](injector: Injector, qualifier: Qualifier, default: () => T) =
    manifest[T].typeArguments.headOption match {
      case Some(manifest) => catching(classOf[TypeNotBoundException]).opt {
          Some(injector.injectAs(qualifier)(manifest.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse None.asInstanceOf[T]
      case None => default()
    }
}

private[processor] class EitherProcessor extends Processor[Either[Any, Any]] {
  def process[T <: Either[_, _] : Manifest](injector: Injector, qualifier: Qualifier, default: () => T) =
    manifest[T].typeArguments match {
      case left :: right :: Nil => catching(classOf[TypeNotBoundException]).opt {
          Right(injector.injectAs(qualifier)(right.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse(catching(classOf[TypeNotBoundException]).opt {
          Left(injector.injectAs(qualifier)(left.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } getOrElse default())
      case _ => default()
    }
}

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

import injector.{Injector, Qualifier}

object `package` {
  val option = new OptionProcessor
}

object Processor {
  def process[T : Manifest](processors: List[Processor[_]], default: () => T, 
                            injector: Injector, qualifier: Qualifier): () => T = {
    processors.foldLeft(default)((default, processor) => {
      if (manifest[T] <:< processor.scope)
        () => processor.asInstanceOf[Processor[T]].process[T](default, injector, qualifier)
      else
        default
    })
  }
}

abstract class Processor[T : Manifest] {
  def scope = manifest[T]
  def process[P <: T : Manifest](default: () => P, injector: Injector, qualifier: Qualifier): P
}

private[processor] class OptionProcessor extends Processor[Option[_]] {
  def process[T <: Option[_] : Manifest](default: () => T, injector: Injector, qualifier: Qualifier) = {
    manifest[T].typeArguments.headOption match {
      case Some(manifest) => {
        try {
          Some(injector.injectAs(qualifier)(manifest.asInstanceOf[Manifest[_ <: AnyRef]])).asInstanceOf[T]
        } catch {
          case e: TypeNotBoundException => None.asInstanceOf[T]
        }
      }
      case None => default()
    }
  }
}

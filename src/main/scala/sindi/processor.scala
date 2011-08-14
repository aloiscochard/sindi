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

import utils.Reflection._

object `package` {
  type Processor[T <: AnyRef] = (Manifest[_], (() => T) => T)

  val option = processor.Processor.create[Option[Any]]((f) => {
    try { f() } catch {
      case e: exception.TypeNotBoundException => None
      case e => throw e
    }
  })
}

object Processor {
  def create[T <: AnyRef : Manifest](f: (() => T) => AnyRef): Processor[AnyRef] = {
    (manifest[T], f).asInstanceOf[processor.Processor[AnyRef]]
  }

  def process[T <: AnyRef : Manifest](processors: List[Processor[AnyRef]], f: () => T): () => T = {
    processors.foldLeft(f)((f, processor) => {
      if (manifest[T] <:< processor._1) { () => processor._2(f).asInstanceOf[T] } else { f }
    })
  }
}

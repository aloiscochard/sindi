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
package context

import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, List, Map}

import injector.{Injector, Binding, Qualifier}
import processor.Processor

trait Context extends Injector {
  lazy val injector: Injector = Injector(build)
  val processors: List[Processor[AnyRef]] = Nil
  protected val bindings: List[binder.binding.Binding[_]] = Nil

  override def injectAs[T <: AnyRef : Manifest](qualifier: Qualifier): T = {
    Processor.process[T](processing, () => injector.injectAs[T](qualifier))(manifest[T])()
  }

  protected def processing: List[Processor[AnyRef]] = processors
  protected def build = bindings.map(_.build.asInstanceOf[Binding])
}

trait Childified extends Context {
  override lazy val injector = Injector(build, () => parent.injector)

  protected val parent: Context

  protected override def processing = {
    @tailrec def collect(context: Context, acc: List[Processor[AnyRef]] = Nil): List[Processor[AnyRef]] = {
      context match {
        case context: Childified => collect(context.parent, context.processors ++ acc)
        case _ => context.processors ++ acc
      }
    }
    collect(this).distinct
  }
}


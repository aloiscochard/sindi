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
  lazy val processors: List[Processor[_]] = processing
  protected val bindings: List[binder.binding.Binding[_]] = Nil

  override def injection[T <: AnyRef : Manifest](qualifier: Qualifier) =
    process[T](qualifier)(injector.injection[T](qualifier))

  override def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean) =
    injector.injectionAll(predicate).map(process[T](qualifier) _)

  protected def processing: List[Processor[_]]
  protected def build = bindings.map(_.build.asInstanceOf[Binding])

  private def process[T <: AnyRef : Manifest](qualifier: Qualifier)(injection: () => T) = 
    Processor.process[T](processors, this, qualifier, injection)(manifest[T])
}

trait Childified extends Context {
  override lazy val injector = Injector(build, parent.injector _)
  protected val parent: Context

  protected override def processing = {
    @tailrec def collect(context: Context, acc: List[Processor[_]] = Nil): List[Processor[_]] = context match {
      case context: Childified => collect(context.parent, context.processors ++ acc)
      case _ => context.processors ++ acc
    }
    collect(this).distinct
  }
}


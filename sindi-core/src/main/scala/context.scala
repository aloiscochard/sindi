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

/** An interface containing operations for contextual object injection. */
trait Context extends Injector {
  /** Return the injector associated with this context. */
  lazy val injector: Injector = Injector(build)
  /** Return the processors associated with this context. */
  lazy val processors: List[Processor[_]] = processing

  /** Return the bindings associated with this context. */
  protected val bindings: List[binder.binding.Binding[_]] = Nil

  override def injectionAs[T <: AnyRef : Manifest](qualifier: Qualifier) =
    process[T](qualifier)(injector.injectionAs[T](qualifier))
  override def injectionAll[T <: AnyRef : Manifest](predicate: Qualifier => Boolean) =
    injector.injectionAll(predicate).map(process[T](qualifier) _)

  /** Return the processors associated with this context and all linked contexts. */
  protected def processing: List[Processor[_]]

  protected def build = bindings.map(_.build.asInstanceOf[Binding])

  private def process[T <: AnyRef : Manifest](qualifier: Qualifier)(injection: () => T) = 
    Processor.process[T](processors, this, qualifier, injection)(manifest[T])
}

/** A trait adding hierarchical link to a [[sindi.context.Context]]. */
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


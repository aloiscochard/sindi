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

import injector.{Binding, Injector, Injection, Qualifiers}
import processor.Processor

/** An interface containing operations for contextual object injection. */
trait Context extends Injector {
  /** Return the injector associated with this context. */
  lazy val injector: Injector = Injector(build)

  /** Return the bindings associated with this context. */
  protected val bindings: List[binder.binding.Binding[_]] = Nil

  override def injectionAs[T <: AnyRef : Manifest](qualifiers: Qualifiers) =
    process[T](qualifiers)(injector.injectionAs[T](qualifiers))
  override def injectionAll[T <: AnyRef : Manifest](qualifiers: Qualifiers) =
    injector.injectionAll(qualifiers).map(process[T](qualifiers) _)

  /** Return the processing associated with this context and all linked contexts. */
  def processors: List[Processor[_]] = Nil

  protected def processing: List[Processor[_]] = processors
  protected def build = bindings.map(_.build.asInstanceOf[Binding])

  private def process[T <: AnyRef : Manifest](qualifiers: Qualifiers)(injection: Injection[T]) = 
    Processor.process[T](processing, this, qualifiers, injection)(manifest[T])
}

/** A trait adding hierarchical link to a [[sindi.context.Context]]. */
trait Childified extends Context {
  override lazy val injector = Injector(build, parent.injector _)
  protected val parent: Context

  override protected def processing = {
    @tailrec def collect(context: Context, acc: List[Processor[_]] = Nil): List[Processor[_]] = context match {
      case context: Childified => collect(context.parent, context.processors ++ acc)
      case _ => context.processors ++ acc
    }
    collect(this).distinct
  }
}


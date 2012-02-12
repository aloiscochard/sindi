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

/** A trait providing operations for object injection using a delegated [[sindi.injector.Injector]],
  * which is encapsulated using a processing system.
  *
  * The processing system is set-up using a list of [[sindi.processor.Processor]].
  */
trait Context extends Injector {
  /** Return the injector associated with this context. */
  lazy val injector: Injector = Injector(bounds)

  /** Return the bindings associated with this context. */
  protected val bindings: Bindings = Nil

  override def injectionAs[T <: AnyRef : Manifest](qualifiers: Qualifiers) =
    process[T](qualifiers)(injector.injectionAs[T](qualifiers))
  override def injectionAll[T <: AnyRef : Manifest](qualifiers: Qualifiers) =
    injector.injectionAll(qualifiers).map(process[T](qualifiers) _)

  /** Return the processors associated with this context. */
  def processors: List[Processor[_]] = Nil

  protected def processing: List[Processor[_]] = processors
  protected def bounds = bindings.map(_.build.asInstanceOf[Binding])

  private def process[T <: AnyRef : Manifest](qualifiers: Qualifiers)(injection: Injection[T]) = 
    Processor.process[T](processing, this, qualifiers, injection)(manifest[T])
}

/** A trait adding hierarchical relationship to a [[sindi.context.Context]]. */
trait Childable extends Context {
  /** Return the injector associated with this context. */
  override lazy val injector = Injector(bounds, parent.injector _)
  protected val parent: Context

  override protected def processing = {
    @tailrec def collect(context: Context, acc: List[Processor[_]] = Nil): List[Processor[_]] = context match {
      case context: Childable => collect(context.parent, context.processors ++ acc)
      case _ => context.processors ++ acc
    }
    collect(this).distinct
  }
}

/** A trait adding wiring capability to a [[sindi.context.Context]]. */
trait Wirable extends Context {
  import java.lang.reflect.Constructor
  import scala.util.control.Exception._
  import exception._

  // TODO [aloiscochard] Improve exception message details

  /** Autowire given function. */
  final def autowire[T <: AnyRef : Manifest, R](f: Function1[T, R]): Function0[R] = {
    val newFunction = (values: List[AnyRef]) => () => f(values(0).asInstanceOf[T]).asInstanceOf[R]
    wireFirst(manifest[T], List(List(manifest[T]) -> newFunction))
  }


  // TODO [aloiscochard] Use templating or scalamacros to support all TupleX / FunctionX
  /** Autowire given tuple type. */
  final def autowireT[T <: AnyRef : Manifest]: Tuple1[T] = {
    val newTuple = (values: List[AnyRef]) => new Tuple1[T](values(0).asInstanceOf[T])
    wireFirst(manifest[T], (List(List(manifest[T]) -> newTuple)))
  }

  final def autowire[T0 <: AnyRef, T1 <: AnyRef, R]
      (f: Function2[T0, T1, R])
      (implicit m0: Manifest[T0], m1: Manifest[T1]): Function0[R] = {

    val newFunction = (values: List[AnyRef]) => () => f(
      values(0).asInstanceOf[T0], values(1).asInstanceOf[T1]
    ).asInstanceOf[R]

    wireFirst(
      manifest[(T0, T1)],
      List(List(manifest[T0], manifest[T1]) -> newFunction)
    )
  }

  final def autowireT[T0 <: AnyRef, T1 <: AnyRef]
      (implicit m0: Manifest[T0], m1: Manifest[T1]): Tuple2[T0, T1] = {
    val newTuple = (values: List[AnyRef]) =>
      (values(0).asInstanceOf[T0], values(1).asInstanceOf[T1])
    wireFirst(
      manifest[(T0, T1)],
      (List(List(manifest[T0], manifest[T1]) -> newTuple))
    )
  }


  protected def wire[T <: AnyRef : Manifest]: Option[T] = catching(classOf[TypeNotBoundException]).opt(inject(manifest[T]))

  private def wireFirst[T <: AnyRef](tpe: Manifest[_], signatures: Seq[Tuple2[List[Manifest[_]], (List[AnyRef]) => T]]): T =
    wireAll[T](signatures).headOption.getOrElse {
      throw new TypeNotBoundException(tpe, " during autowiring")
    }

  private def wireAll[T](signatures: Seq[Tuple2[List[Manifest[_]], (List[AnyRef]) => T]]): Seq[T] = {
    signatures.view.flatMap {
      case (parameters, constructor) => {
        // TODO [aloiscochard] Cache wire calls
        val values = parameters.toList.flatMap((m) => wire(m.asInstanceOf[Manifest[AnyRef]]))
        if (values.size == parameters.size) Some(constructor(values)) else None
      }
    }
  }
}


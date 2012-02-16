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

  override def injectionAs[T : Manifest](qualifiers: Qualifiers) =
    process[T](qualifiers)(injector.injectionAs[T](qualifiers))
  override def injectionAll[T : Manifest](qualifiers: Qualifiers) =
    injector.injectionAll(qualifiers).map(process[T](qualifiers) _)

  /** Return the processors associated with this context. */
  def processors: List[Processor[_]] = Nil

  protected def processing: List[Processor[_]] = processors
  protected def bounds = bindings.map(_.build.asInstanceOf[Binding])

  private def process[T : Manifest](qualifiers: Qualifiers)(injection: Injection[T]) = 
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
trait Wirable extends Context with WirableBoilerplate {
  import java.lang.reflect.Constructor
  import scala.util.control.Exception._
  import exception._

  // TODO [aloiscochard] Improve exception message details

  /** Autowire given function. */
  final def autowire[T : Manifest, R](f: Function1[T, R]): R = {
    val newFunction = (values: List[Any]) => () => f(values(0).asInstanceOf[T]).asInstanceOf[R]
    wireFirst(manifest[T], List(List(manifest[T]) -> newFunction)).apply
  }


  /** Autowire given tuple type. */
  final def autowireT[T : Manifest]: Tuple1[T] = {
    val newTuple = (values: List[Any]) => new Tuple1[T](values(0).asInstanceOf[T])
    wireFirst(manifest[T], (List(List(manifest[T]) -> newTuple)))
  }

  protected def wire[T : Manifest]: Option[T] = catching(classOf[TypeNotBoundException]).opt(inject(manifest[T]))

  private[context] def wireFirst[T]
      (tpe: Manifest[_], signatures: Seq[Tuple2[List[Manifest[_]], (List[Any]) => T]]): T =
    wireAll[T](signatures).headOption.getOrElse {
      throw new TypeNotBoundException(tpe, " during autowiring")
    }

  private[context] def wireAll[T]
      (signatures: Seq[Tuple2[List[Manifest[_]], (List[Any]) => T]]): Seq[T] = {
    signatures.view.flatMap {
      case (parameters, constructor) => {
        // TODO [aloiscochard] Cache wire calls
        val values = parameters.toList.flatMap((m) => wire(m.asInstanceOf[Manifest[Any]]))
        if (values.size == parameters.size) Some(constructor(values)) else None
      }
    }
  }
}


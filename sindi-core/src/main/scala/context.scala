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
trait Childable extends Context {
  override lazy val injector = Injector(build, parent.injector _)
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

  // TODO [aloiscochard] Improve exception message details

  final def autowire[T <: AnyRef : Manifest]: T = {
    // TODO [aloiscochard] support companion methods + inner class + java statics + filter tuple and others jewel
    val create = (constructor: Constructor[_]) => (values: List[AnyRef]) => constructor.newInstance(values:_*).asInstanceOf[T]
    val constructors = manifest[T].erasure.getConstructors.toList.map((c) => 
        (c.getParameterTypes.toList.map(clazz => Manifest.classType[AnyRef](clazz)), create(c)))
      wireFirst[T](manifest[T], constructors)
  }

  final def autowire[T <: AnyRef : Manifest, R](f: Function[T, R]): Function0[R] = {
    val newFunction = (values: List[AnyRef]) => () => f(values(0).asInstanceOf[T]).asInstanceOf[R]
    wireFirst[Function0[R]](manifest[T], List(List(manifest[T]) -> newFunction))
  }

  // TODO [aloiscochard] Use templating or scalamacros to support all TupleX / FunctionX
  final def autowireT[T <: Tuple2[_, _] : Manifest]: T = {
    val newTuple = (values: List[AnyRef]) => (values(0), values(1)).asInstanceOf[T]
    wireFirst[T](manifest[T], (List(manifest[T].typeArguments -> newTuple)))
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


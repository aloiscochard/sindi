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

import scala.collection.immutable.{HashMap, List, Map}

import injector.Injector

trait Context extends Injector {
  lazy val injector = Injector(build)
  protected val bindings: List[sindi.binder.binding.Binding[_]] = Nil
  override def injectAs[T <: AnyRef : Manifest](qualifier: AnyRef): T = injector.injectAs[T](qualifier)
  // Move to binder/bindings ?
  protected def build = bindings.map(_.build.asInstanceOf[Tuple2[Tuple2[AnyRef, Class[_]], () => AnyRef]]).toMap
}

trait Childified extends Context {
  override lazy val injector = Injector(build, () => parent.injector)
  protected val parent: Context
}

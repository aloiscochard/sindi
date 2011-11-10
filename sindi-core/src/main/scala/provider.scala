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
package provider

trait Provider[T <: AnyRef] extends Function0[T] {
  val signature: Manifest[T]
  def apply(): T
}

object Provider {
  def apply[T <: AnyRef : Manifest](f: => T): Provider[T] = apply(manifest[T])(f)
  def apply[T <: AnyRef](manifest: Manifest[T])(f: => T) = new Provider[T] {
    val signature: Manifest[T] = manifest
    def apply() = f
  }
  def cached[T <: AnyRef : Manifest](f: => T): Provider[T] = {
    lazy val cache = f
    apply(manifest[T])(cache)
  }
}

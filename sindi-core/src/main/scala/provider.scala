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

/** An injection provider. */
trait Provider[T <: AnyRef] extends Function0[T] {
  /** Return the signature (scope defining which type can be applied to this provider). */
  val signature: Manifest[T]
  def apply(): T
}

/** A companion object containing facilities to create injection providers. */
object Provider {
  /** Return a new provider for a given function (provider signature is defined using implicit manifest). */
  def apply[T <: AnyRef : Manifest](f: => T): Provider[T] = new Provider[T] {
    val signature: Manifest[T] = manifest[T]
    def apply() = f
  }
  /** Return a new cached provider for a given function.
   *
   * Function is applied only once when the provider is called the first time,
   * the function return value is then cached for future calls*/
  def cached[T <: AnyRef : Manifest](f: => T): Provider[T] = {
    lazy val cache = f
    apply(cache)(manifest[T])
  }
}

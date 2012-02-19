package sindi.examples.userguide.context

import sindi._
import java.net.URI

object Formatter {
  def name(n: String) = "Name: " + n
  def website(u: URI) = "Website: " + u
}

case class Info(name: String, website: URI)

object AppContext extends Context {
  import Formatter._

  override val bindings = Bindings(
    bind[String] to "sindi",
    bind[URI] to new URI("http://aloiscochard.github.com/sindi/")
  )

  val info = autowire(Info.apply _)

  val infoT = autowire((_: String, _: URI))

  val description = List(autowire(name _), autowire(website _)).mkString(", ")
}

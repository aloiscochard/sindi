package sindi.examples.demo

import sindi._

import dao._

object Application extends App with Context with TaskComponent with UserComponent {
  implicit val context = this
  override val modules = (new DaoModule) :: Nil 

  /*
  override val bindings: Bindings = bind[store.User] to user

  private lazy val user = new store.User with store.RemoteStore
  */

  println(tasks.get)
}

package sindi.examples.demo
package application.embedded

import sindi._

import application._
import model._
import repository._
import services._

object Bootstrap extends App with Context {
  override lazy val modules = new RepositoryModule :: new UserServiceModule :: Nil

  override val bindings: Bindings = bind[User] to User("webmaster", "Webmaster") as qualifier[Webmaster]

  new ComponentContext with Application with UserServiceComponent with TaskServiceComponent
}



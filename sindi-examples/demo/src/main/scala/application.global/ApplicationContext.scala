package sindi.examples.demo
package application.global

import sindi._

import store._
import model._
import repository._
import services._

object ApplicationContext extends Context { 
  override lazy val modules = new RepositoryModule :: new UserServiceModule :: Nil
  override val bindings: Bindings = bind[Store[User]] to new Store[User] with DiskStore[User]
}

trait ApplicationComponent extends ComponentWith[ApplicationContext.type] {
  override val context = ApplicationContext
}

package sindi.examples.demo
package application.embedded

import sindi._

import application._
import repository._
import services._

object Bootstrap extends App with Context {
  override lazy val modules = new RepositoryModule :: Nil
  new ComponentContext with Application with UserServiceComponent with TaskServiceComponent
}



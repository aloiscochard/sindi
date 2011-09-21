package sindi.examples.demo
package application.global

import sindi._

import application._
import services._

object Bootstrap extends App {
  // TODO Compiler should check it mixed in component dependencies are satisfed
  new ApplicationComponent with Application with UserServiceComponent with TaskServiceComponent
}

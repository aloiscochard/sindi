package sindi.examples.demo
package application.global

import sindi._

import application._
import services._

object Bootstrap extends App {
  new ApplicationComponent with Application with UserServiceComponent with TaskServiceComponent
}

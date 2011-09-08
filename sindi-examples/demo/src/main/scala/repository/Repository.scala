package sindi.examples.demo
package repository

import model._
import store._

case class Repository(users: Store[User], tasks: Store[Task]) 

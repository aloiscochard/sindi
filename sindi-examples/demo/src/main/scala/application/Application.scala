package sindi.examples.demo
package application

import sindi._

import model._
import services._

trait Application extends UserService with TaskService {
  tasks.load(Task("OSS World Domination", "TBD"))
  users.load(User("aloiscochard", "Alois Cochard"))

  println("tasks.get == %s".format(tasks.get))
  println("users.get == %s".format(users.get))
  println("administrator == %s".format(administrator))
  println("webmaster == %s".format(webmaster))
}

package sindi.examples.demo

import sindi._

import dao._

object Application extends App with Context with TaskComponent with UserComponent {
  implicit val context = this
  override lazy val modules = new DaoModule :: Nil

  tasks.load(Task("OSS World Domination", "TBD"))
  users.load(User("aloiscochard", "Alois Cochard"))

  println(tasks.get)
  println(users.get)
}

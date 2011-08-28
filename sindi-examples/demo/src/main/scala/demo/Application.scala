package sindi.examples.demo

import sindi._

import dao._

object Application extends App with Context {
  implicit val context = this
  override lazy val modules = new DaoModule :: Nil

  val tasks = from[DaoModule].tasks
  val users = from[DaoModule].users

  tasks.load(Task("OSS World Domination", "TBD"))
  users.load(User("aloiscochard", "Alois Cochard"))

  println(tasks.get)
  println(users.get)
}

object ApplicationWithComponents extends App with Context with UserComponent with TaskComponent {
  implicit val context = this
  override lazy val modules = new DaoModule :: Nil

  tasks.load(Task("OSS World Domination", "TBD"))
  users.load(User("aloiscochard", "Alois Cochard"))

  println(tasks.get)
  println(users.get)
}

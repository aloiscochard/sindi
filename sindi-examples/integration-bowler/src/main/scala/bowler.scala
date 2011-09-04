package sindi.examples.integration.bowler

import sindi._

import org.bowlerframework.controller.{FunctionNameConventionRoutes, Controller}
import org.bowlerframework.view.Renderable

import sindi.examples.demo.dao._

class Bootstrap extends Context with UserComponent with TaskComponent {
  override lazy val modules = new DaoModule :: Nil
  val indexController = new ComponentContext with IndexController
  val tasksController = new ComponentContext with TasksController
  users.load(User("aloiscochard", "Alois Cochard"))
  tasks.load(Task("My Task", "..."))
}

trait IndexController extends Controller with FunctionNameConventionRoutes with Renderable with UserComponent {
  def `GET /` = render("user" -> users.get)
}

trait TasksController extends Controller with FunctionNameConventionRoutes with Renderable with TaskComponent {
  def `GET /tasks` = render("task" -> tasks.get)
}

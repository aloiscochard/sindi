package sindi.examples.integration.bowler

import sindi._

import org.bowlerframework.controller.{FunctionNameConventionRoutes, Controller}
import org.bowlerframework.view.Renderable

import sindi.examples.demo.dao._

class Bootstrap extends Context with UserComponent with TaskComponent {
  override lazy val modules = new DaoModule :: Nil
  val indexController = new IndexController
  val tasksController = new TasksController
  users.load(User("aloiscochard", "Alois Cochard"))
  tasks.load(Task("My Task", "..."))
}

class IndexController(implicit override val context: Context) extends ControllerWithContext with UserComponent {
  def `GET /` = render("user" -> users.get)
}

class TasksController(implicit override val context: Context) extends ControllerWithContext with TaskComponent {
  def `GET /tasks` = render("task" -> tasks.get)
}

abstract class ControllerWithContext(implicit override val context: Context) extends Controller
  with ComponentWithContext with FunctionNameConventionRoutes with Renderable with UserComponent 

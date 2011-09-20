package sindi.examples.integration.bowler

import sindi._

import org.bowlerframework.controller.{FunctionNameConventionRoutes, Controller}
import org.bowlerframework.view.Renderable

import sindi.examples.demo.model._
import sindi.examples.demo.services._
import sindi.examples.demo.repository.RepositoryModule

class Bootstrap extends Context with UserServiceComponent with TaskServiceComponent {
  override lazy val modules = new RepositoryModule :: Nil
  val indexController = new ComponentContext with IndexController
  val tasksController = new ComponentContext with TasksController
  users.load(User("aloiscochard", "Alois Cochard"))
  tasks.load(Task("My Task", "..."))
}

trait IndexController extends Controller with FunctionNameConventionRoutes with Renderable
    with UserServiceComponent {
  def `GET /` = render("user" -> users.get)
}

trait TasksController extends Controller with FunctionNameConventionRoutes with Renderable
    with TaskServiceComponent {
  def `GET /tasks` = render("task" -> tasks.get)
}

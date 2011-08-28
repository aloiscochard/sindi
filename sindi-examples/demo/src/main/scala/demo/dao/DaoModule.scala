package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil
   
  def users = from[StoreModule[User]].inject[Store[User]]
  def tasks = from[StoreModule[Task]].inject[Store[Task]]
}

trait UserComponent extends Component[DaoModule] { lazy val users = from[DaoModule].users }

trait TaskComponent extends Component[DaoModule] { def tasks = from[DaoModule].inject[Store[Task]] }

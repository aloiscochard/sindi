package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil
   
  lazy val users = from[StoreModule[User]].inject[Store[User]]
  lazy val tasks = from[StoreModule[Task]].inject[Store[Task]]
}

trait UserComponent extends Component[DaoModule] { def users = from[DaoModule].users }

trait TaskComponent extends Component[DaoModule] { def tasks = from[DaoModule].tasks }

package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil
}

trait UserComponent extends Component[DaoModule] {
  lazy val users = from[DaoModule].from[StoreModule[User]].inject[Store[User]]
}

trait TaskComponent extends Component[DaoModule] {
  lazy val tasks = from[DaoModule].from[StoreModule[Task]].inject[Store[Task]]
}

package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil

  lazy val users = from[StoreModule[User]].inject[MemoryStore[User]]
  lazy val tasks = from[StoreModule[Task]].inject[MemoryStore[Task]]

  override val bindings = Bindings(
    bind[Store[User]] to users,
    bind[Store[Task]] to tasks
  )
}

trait UserComponent extends Component {
  lazy val users = from[DaoModule].inject[Store[User]]
}

trait TaskComponent extends Component {
  lazy val tasks = from[DaoModule].inject[Store[Task]]
}

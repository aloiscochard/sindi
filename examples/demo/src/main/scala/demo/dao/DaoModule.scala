package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override val modules = StoreModule.of[Task](this) :: StoreModule.of[User](this) :: Nil
}

trait TaskComponent extends Component {
  val tasks = from[StoreModule[Task]].inject[Store[Task]]
}

trait UserComponent extends Component {
  val users = from[StoreModule[User]].inject[Store[User]]
}


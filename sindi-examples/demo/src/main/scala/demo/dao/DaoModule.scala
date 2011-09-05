package sindi.examples.demo
package dao

import sindi._

import store._

class DaoModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil
   
  lazy val users = from[StoreModule[User]].store
  lazy val tasks = from[StoreModule[Task]].store
}

trait DaoComponent extends Component { val `sindi.examples.demo.dao` = new ModuleManifest[DaoModule] }

trait UserComponent extends DaoComponent { def users = from[DaoModule].users }

trait TaskComponent extends DaoComponent { def tasks = from[DaoModule].tasks }

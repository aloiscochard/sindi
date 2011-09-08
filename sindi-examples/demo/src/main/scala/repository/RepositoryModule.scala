package sindi.examples.demo
package repository

import sindi._

import model._
import store._

class RepositoryModule(implicit val context: Context) extends Module { 
  override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil

  lazy val repository = Repository(
    from[StoreModule[User]].store,
    from[StoreModule[Task]].store
  )
}

trait RepositoryComponent extends Component {
  val `sindi.examples.demo.repository` = new ModuleManifest[RepositoryModule]
  def repository = from[RepositoryModule].repository
}



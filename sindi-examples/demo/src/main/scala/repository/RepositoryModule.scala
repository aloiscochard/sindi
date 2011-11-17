package sindi.examples.demo
package repository

import sindi._

import model._
import store._

final class RepositoryModule(implicit context: Context) extends Module { 
  //override lazy val modules = new StoreModule[Task](this) :: new StoreModule[User](this) :: Nil
  override lazy val modules = new StoreModule[Task] :: new StoreModule[User] :: Nil

  lazy val repository = Repository(from[StoreModule[User]].store, from[StoreModule[Task]].store)
}

trait RepositoryComponent extends Component {
  def repository = from[RepositoryModule].repository
}



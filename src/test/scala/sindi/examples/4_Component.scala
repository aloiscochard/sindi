package sindi.examples.component

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context with consumer.ConsumerComponent {
  import sindi.examples.component.consumer.ConsumerModule

  override val modules = sindi.examples.component.consumer.ConsumerModule(this) :: Nil

  override val bindings = (bind[store.User] to new store.User with store.RemoteStore scope singleton).build :: Nil

  consumer.start
}

/////////////////////
// Consumer Module //
/////////////////////

package consumer {
  import sindi.examples.component.store._

  object ConsumerModule extends ModuleFactory[ConsumerModule]

  class ConsumerModule(implicit val context: Context) extends Module { 
      override val modules = StoreModule(this) :: Nil

      override val bindings = (bind[Consumer] to new ComponentContext(this) with Consumer).build :: Nil
  }
  
  trait ConsumerComponent extends Component { lazy val consumer = from[ConsumerModule].inject[Consumer] }

  trait Consumer extends UserStore with UserPreferenceStore {
    def start() = {
      print("users: ")
      users.store()
      println("")

      print("userPreferences: ")
      userPreferences.store()
      println("")
    }
  }
}

//////////////////
// Store Module //
//////////////////

package store {
  object StoreModule extends ModuleFactory[StoreModule]

  class StoreModule(implicit context: Context) extends Module {
    override val bindings = 
      (bind[User] to new User with MemoryStore scope singleton).build ::
      (bind[UserPreference] to new UserPreference with MemoryStore scope singleton).build ::
      Nil
  }

  trait UserStore extends Component { lazy val users = from[StoreModule].inject[User] }
  trait UserPreferenceStore extends Component { lazy val userPreferences = from[StoreModule].inject[UserPreference] }

  trait Store { def store() }

  trait MemoryStore extends Store { def store() = print("memory") }
  trait RemoteStore extends Store { def store() = print("remote") }

  trait User extends Store
  trait UserPreference extends Store
}

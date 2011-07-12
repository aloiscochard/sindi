package sindi.examples.component

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  import store._
  StoreModule.childify(this)
  define {
    bind[User] to new User with RemoteStore scope singleton
  }
  new Consumer().start()
}

class Consumer extends store.UserStore with store.UserPreferenceStore {
  def start() = {
    print("users: ")
    this.users.store()
    println("")

    print("userPreferences: ")
    this.userPreferences.store()
    println("")
  }
}

package store {

  object StoreModule extends ModuleFactory with Environment {
    define {
      bind[User] to new User with MemoryStore scope singleton
      bind[UserPreference] to new UserPreference with MemoryStore scope singleton
    }
  }

  trait StoreComponent extends Component { override protected lazy val injector = StoreModule.injector }

  trait UserStore extends StoreComponent { lazy val users = inject[User] }
  trait UserPreferenceStore extends StoreComponent { lazy val userPreferences = inject[UserPreference] }

  trait Store { def store() }

  trait MemoryStore extends Store { def store() = print("memory") }
  trait RemoteStore extends Store { def store() = print("remote") }

  trait User extends Store
  trait UserPreference extends Store

}

package sindi.examples.idiomatic

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  trait AppComponent extends Component { override protected lazy val injector = Application.injector }

  import store._

  include(StoreModule(this))

  define {
    bind[User] to new User with RemoteStore scope singleton
  }

  (new Consumer with AppComponent).start
}


trait Consumer extends store.UserStore with store.UserPreferenceStore {
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

  object StoreModule extends ModuleFactory {
    define {
      bind[User] to new User with MemoryStore scope singleton
      bind[UserPreference] to new UserPreference with MemoryStore scope singleton
    }
  }

  trait UserStore extends Component { lazy val users = inject[User] }
  trait UserPreferenceStore extends Component { lazy val userPreferences = inject[UserPreference] }

  trait Store { def store() }

  trait MemoryStore extends Store { def store() = print("memory") }
  trait RemoteStore extends Store { def store() = print("remote") }

  trait User extends Store
  trait UserPreference extends Store

}

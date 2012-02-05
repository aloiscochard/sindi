package sindi.examples.userguide.component

package app {
  import sindi._
  import user.service.module._

  object AppContext extends Context {
    override lazy val modules = new UserServiceModule :: Nil 
  }

  trait AppComponent extends ComponentWith[AppContext.type] with UserServiceComponent {
    override val context = AppContext
  }
}


package user {
  package repository {
    package module {
      import sindi._

      final class UserRepositoryModule(implicit context: Context) extends Module {
        override val bindings: Bindings =
          bind[UserRepository] to new DefaultUserRepository

        def repository = inject[UserRepository]
      }

      trait UserRepositoryComponent extends Component {
        def repository = from[UserRepositoryModule].repository
      }
    }

    trait UserRepository
    class DefaultUserRepository extends UserRepository
    class AdvancedUserRepository extends UserRepository
  }

  package service {
    import repository._

    package module {
      import sindi._
      import repository.module._

      final class UserServiceModule(implicit context: Context) extends Module {
        override lazy val modules = new UserRepositoryModule :: Nil

        override val bindings: Bindings =
          bind[UserService] to
            new ComponentContext with UserRepositoryComponent with DefaultUserService

        def service = inject[UserService]
      }

      trait UserServiceComponent extends Component {
        def userService = from[UserServiceModule].service
      }
    }


    trait UserService {
      def repository: UserRepository
      def name: String
    }
    trait DefaultUserService extends UserService  { def name = "default" }
    trait AdvancedUserService extends UserService  { def name = "advanced" }
  }

}

package sindi.examples.basic

import org.specs2.mutable._

import sindi._
import sindi.examples.basic.user._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  object Mode extends Enumeration {
    type Mode = Value;
    val Advanced, Default = Value
  }

  import Mode._

  val mode : Mode = args match {
    case Array("advanced") => Advanced
    case _ => Default
  }

  val userService = new UserServiceComponent(this)

  define {
    Application.mode match {
      case Mode.Advanced => bind[UserRepository] to new UserRepository with Advanced scope singleton
      case _ => 
    }
  }

  userService().start()
}

//////////////////
// User Service //
//////////////////

package user {

  class UserServiceComponent(context: Context) extends Component[UserService](context: Context) {
    define {
      bind[UserService] to new DefaultUserService(inject[UserRepository]) scope singleton
      bind[UserRepository] to new UserRepository with Default scope singleton
    }
  }

  trait UserService { def start() }

  class DefaultUserService(val repository: UserRepository) extends UserService {
    println("DefaultUserService.constructor")
    def start() = {
      println("DefaultUserService.start")
      repository.start()
    }
  }

  abstract class UserRepository { def start() }

  trait Default extends UserRepository{
    println("DefaultUserRepository.constructor")
    def start() = println("DefaultUserRepository.start")
  }

  trait Advanced extends UserRepository{
    println("AdvancedUserRepository.constructor")
    def start() = println("AdvancedUserRepository.start")
  }

  class DefaultUserServiceSpec extends Specification {
    class MockedUserRepository extends UserRepository {
      println("MockedUserRepository.constructor")
      def start() = println("MockedUserRepository.start")
    }

    "DefaultUserService" should {
      "be mockable" in {
        val repository = new MockedUserRepository
        val service = new DefaultUserService(repository)
        service.start
        service.repository must be equalTo repository
      }
    }
  }
}

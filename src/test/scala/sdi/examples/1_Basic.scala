package sdi.examples.basic

import org.specs2.mutable._

import sdi.Context
import sdi.examples.basic.user._

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

  define {
    bind[UserService] to new DefaultUserService(this) scope singleton
    bind[UserRepository] to { Application.mode match {
      case Mode.Advanced => new AdvancedUserRepository
      case _ => new DefaultUserRepository
    } } scope singleton

  }

  inject[UserService].start()
}

//////////////////
// User Service //
//////////////////

package user {
  trait UserService { def start() }

  class DefaultUserService(val repository: UserRepository) extends UserService with Context {
    def this(context: Context) = {
      this(context.inject[UserRepository])
      childify(context)
    }
    println("DefaultUserService.constructor")
    def start() = {
      println("DefaultUserService.start")
      repository.start()
    }
  }

  trait UserRepository { def start() }

  class DefaultUserRepository extends UserRepository{
    println("DefaultUserRepository.constructor")
    def start() = println("DefaultUserRepository.start")
  }

  class AdvancedUserRepository extends UserRepository{
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

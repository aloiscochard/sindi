package sdi.examples.module

import org.specs2.mutable._

import sdi.Context

trait UserService { def start() }

object UserService extends Context {
  define {
      bind[UserService] to new DefaultUserService scope singleton
      bind[UserRepository] to new DefaultUserRepository scope singleton
  }
}

import UserService.inject

class DefaultUserService(val repository: UserRepository = inject[UserRepository]) extends UserService {
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

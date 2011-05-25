package sdi.exemples.application

import sdi.Context

import modules._

object Application extends App {
  ApplicationContext.inject[UserService].start()
}

object ApplicationContext extends Context {
  define {
    bind[UserService] to new DefaultUserService scope singleton
    bind[UserRepository] to new DefaultUserRepository scope singleton
  }
}

package modules {
  import sdi.exemples.application.ApplicationContext._

  trait UserService { def start() }
  
  class DefaultUserService extends UserService {
    lazy val repository = inject[UserRepository]
    println("DefaultUserService.constructor")
    def start() = {
      println("DefaultUserService.start")
      repository.start()
    }
  }

  trait UserRepository { def start() }

  class DefaultUserRepository extends UserRepository{
    println("DefaultUserRepository.constructor")
    def start() = {
      println("DefaultUserRepository.start")
    }
  }

}

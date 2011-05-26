package sdi.examples.application

import sdi.examples.module._

object Application extends App with sdi.Context {

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
    Application.mode match {
      case Mode.Advanced => {
        bind[UserRepository] to new AdvancedUserRepository scope singleton
      }
    }
  }

  UserService.childify(this)
  UserService.start()
}


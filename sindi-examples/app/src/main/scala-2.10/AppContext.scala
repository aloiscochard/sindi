package sindi.examples.app

import sindi.core._
import sindi.config._

trait AppContext extends Configurable {

  object AppConfiguration extends Configuration {
    object Database extends Section("database") {
      val host      = Key[String]("host")
      val port      = Key[Int]   ("port")
      val username  = Key[String]("username")
      val password  = Key[String]("password")
    }
  }

  implicit val validated = AppConfiguration.validate()
  import AppConfiguration._

  implicit val databaseInfo = bind {
    import Database._
    DatabaseInfo(host, port, username, password)
  }

  implicit val databaseClient = bind(profile match {
    case Some("standalone") => new InMemoryDatabaseClient()
    case _ => autowire[SqlDatabaseClient]
  })

  implicit val databaseClientSession = as[Repository[Session]].bind(new InMemoryDatabaseClient())

  implicit val documentRepository = bind(autowire[Repository[Document]])
  implicit val sessionRepository = bind(autowire[Repository[Session]])
  implicit val userRepository = bind(autowire[Repository[User]])

  implicit val contentService = bind(autowire[DefaultContentService])
  implicit val sessionService = bind(autowire[DefaultSessionService])
}

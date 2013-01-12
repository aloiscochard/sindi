package sindi.examples.app

case class DatabaseInfo(host: String, port: Int, username: String, password: String)

trait DatabaseClient
class SqlDatabaseClient(info: DatabaseInfo) extends DatabaseClient { override def toString = s"SqlDatabaseClient(${info})" }
class InMemoryDatabaseClient() extends DatabaseClient { override def toString = "InMemoryDatabaseClient" }

trait Entity
trait Document extends Entity
trait Session extends Entity
trait User extends Entity

class Repository[E <: Entity](databaseClient: DatabaseClient) {
  override def toString = s"${this.getClass.getSimpleName}($databaseClient)"
}

trait ContentService
class DefaultContentService(users: Repository[User], documents: Repository[Document]) extends ContentService {
  override def toString = s"DefaultContentService($users, $documents)"
}

trait SessionService
class DefaultSessionService(sessions: Repository[Session]) extends SessionService {
  override def toString = s"DefaultSessionService($sessions)"
}


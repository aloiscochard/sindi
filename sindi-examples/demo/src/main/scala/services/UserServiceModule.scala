package sindi.examples.demo
package services

import sindi._

import model._
import repository._

class UserServiceModule(implicit val context: Context) extends Module { 
  override val bindings: Bindings =
    bind[User] to User("administrator", "Administrator") as qualifier[Administrator]
}

trait UserServiceComponent extends RepositoryComponent with UserService {
  override def administrator = user(qualifier[Administrator])
  override def webmaster = user(qualifier[Webmaster] || qualifier[Administrator])

  private lazy val user = from[UserServiceModule].injectAs[User] _
}

trait Administrator extends UserQualifier
trait Webmaster extends UserQualifier
protected sealed trait UserQualifier

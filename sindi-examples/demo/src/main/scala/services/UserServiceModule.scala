package sindi.examples.demo
package services

import sindi._

import model._
import repository._

class UserServiceModule(implicit val context: Context) extends Module { 
  override val bindings: Bindings =
    bind[User] to User("administrator", "Administrator") as qualifier[Administrator]

  def administrator = injectAs[User](qualifier[Administrator])
  def webmaster = injectAs[User](qualifier[Webmaster] || qualifier[Administrator])
}

trait UserServiceComponent extends RepositoryComponent with UserService {
  override def administrator = from[UserServiceModule].administrator
  override def webmaster = from[UserServiceModule].webmaster
}

trait Administrator extends UserQualifier
trait Webmaster extends UserQualifier
protected sealed trait UserQualifier

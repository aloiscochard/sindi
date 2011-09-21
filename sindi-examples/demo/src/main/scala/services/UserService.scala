package sindi.examples.demo
package services

import model._
import repository._
import store._

trait UserService {
  def users: Store[User] = repository.users
  def repository: Repository
  def administrator: User
  def webmaster: User
}

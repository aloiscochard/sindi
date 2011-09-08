package sindi.examples.demo
package services

import model._
import repository._
import store._

trait TaskService {
  def tasks: Store[Task] = repository.tasks
  def repository: Repository
}

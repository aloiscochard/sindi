package sindi.examples.demo
package application.global

import sindi._

import repository._

object ApplicationContext extends Context { override lazy val modules = new RepositoryModule :: Nil }

trait ApplicationComponent extends ComponentWith[ApplicationContext.type] { override val context = ApplicationContext }

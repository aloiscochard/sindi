package sindi.examples.app

import sindi.core._

object App extends scala.App with AppContext {
  println(inject[ContentService])
  println(inject[SessionService])
}


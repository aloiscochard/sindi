package sindi.examples.introduction

object Introduction {
  import sindi.Sindi._

  class Component
  class UID

  define {
    bind[Component] to new Component scope singleton
    bind[UID] to new UID
  }

  def intro() {
      for (x <- 1 to 4) {
        println(inject[Component])
        println(inject[UID])
      }
  }

  object Functional extends sindi.Context {
    define {
      var i = 1
      bind[String] to { i = i + i; "i=" + i }
    }

    def haveFun = {
      for (x <- 1 to 4) {
        println(inject[String])
      }
    }
  }
}

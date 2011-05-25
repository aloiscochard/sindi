package sdi.exemples.introduction


object Introduction {
  import sdi.SDI._

  class Component
  class UID

  define {
    val c = new Component
    bind[Component] to c
    bind[UID] to new UID
  }

  def intro() {
      for (x <- 1 to 4) {
        println(inject[Component])
        println(inject[UID])
      }
  }
  

  object Functional extends sdi.Context {
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

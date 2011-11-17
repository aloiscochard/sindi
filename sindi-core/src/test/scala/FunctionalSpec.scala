//      _____         ___                       
//     / __(_)__  ___/ (_)                      
//    _\ \/ / _ \/ _  / /                       
//   /___/_/_//_/\_,_/_/                        
//                                              
//  (c) 2011, Alois Cochard                     
//                                              
//  http://aloiscochard.github.com/sindi        
//                                              

package sindi

import org.specs2.mutable._

import exception._

class FunctionalSpec extends Specification {

  "Sindi Context" should {
    "throw an exception when type is not bound" in {
      class Foo extends Context
      new Foo().inject[String] must throwAn[TypeNotBoundException]
    }

    "bind concrete type" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      new Foo().inject[String] mustEqual "sindi"
    }

    "bind concrete type and cache" in {
      class Foo extends Context {
        override val bindings = Bindings(bind[Object] to new Object)
      }
      val foo = new Foo
      foo.inject[Object] mustEqual foo.inject[Object]
    }


    "bind concrete type with qualifier" in {
      class Foo extends Context { override val bindings = Bindings(bind[String] to "sindi" as "sindi",
                                                                   bind[String] to "scala" as qualifier[Foo])
      }
      val foo = new Foo
      foo.inject[String] must throwAn[TypeNotBoundException]
      foo.injectAs[String](qualifier[Int]) must throwAn[TypeNotBoundException]
      foo.injectAs[String]("sindi") mustEqual "sindi"
      foo.injectAs[String](qualifier[Foo]) mustEqual "scala"
    }

    "bind concrete type with combined qualifier" in {
      class Foo extends Context { override val bindings = Bindings(bind[String] to "ioc",
                                                                   bind[String] to "sindi" as "sindi",
                                                                   bind[String] to "scala" as qualifier[Foo])
      }
      val foo = new Foo
      ("a" == "b") || ("b" == "b") mustEqual true
      foo.injectAs[String]("sindi" or qualifier[Foo]) mustEqual "sindi"
      foo.injectAs[String](qualifier[Foo] or "sindi") mustEqual "scala"
      foo.injectAs[String](qualifier[String] or "scala" or None) mustEqual "ioc"
      foo.injectAs[String](qualifier[String] or "sindi" or None) mustEqual "sindi"
      foo.injectAs[String](qualifier[Foo] || "sindi") mustEqual "scala"
      foo.injectAs[String](qualifier[String] || "scala" || None) mustEqual "ioc"
      foo.injectAs[String](qualifier[String] || "sindi" || None) mustEqual "sindi"
    }

    "bind concrete type to provider" in {
      class Foo extends Context {
        override val bindings = Bindings(bind[Object] to provider { new Object })
      }
      val foo = new Foo
      foo.inject[Object] mustNotEqual foo.inject[Object]
    }

    "bind concrete type with scope" in {
      class Bar
      var state = 1
      class Foo extends Context { override val bindings: Bindings = bind[Bar] to provider { new Bar } scope { state } }
      val foo = new Foo
      val bar1 = foo.inject[Bar].hashCode
      bar1 mustEqual foo.inject[Bar].hashCode
      state = 2
      bar1 mustNotEqual foo.inject[Bar].hashCode
    }

    "bind abstract type" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      new Foo().inject[AnyRef] mustEqual "sindi"
    }

    "bind abstract type with FIFO priority" in {
      class FooA extends Context { override val bindings = Bindings(bind[AnyRef] to "sindi") }
      val fooA = new FooA
      fooA.inject[AnyRef] mustEqual "sindi"
      fooA.inject[String] must throwAn[TypeNotBoundException]

      class FooB extends Context { override val bindings = Bindings(bind[List[String]] to List("sindi"),
                                                                    bind[String] to "sindi",
                                                                    bind[AnyRef] to "scala") }
      val fooB = new FooB
      fooB.inject[String] mustEqual "sindi"
      fooB.inject[AnyRef] mustEqual List("sindi")
    }

    "bind parameterized type" in {
      val list = List("sindi")
      class FooA extends Context { override val bindings: Bindings = bind[List[String]] to list }
      val fooA = new FooA
      fooA.inject[List[String]] mustEqual list
      fooA.inject[List[AnyRef]] mustEqual list

      class FooB extends Context { override val bindings: Bindings = bind[List[AnyRef]] to list }
      new FooB().inject[List[String]] must throwAn[TypeNotBoundException]
    }

    "support Option" in {
      class FooA extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      val fooA = new FooA
      fooA.inject[Option[String]] mustEqual Some("sindi")
      fooA.inject[Option[List[String]]] mustEqual None
      fooA.inject[String] mustEqual "sindi"

      class FooB extends Context 
      new FooB().inject[Option[String]] mustEqual None
    }

    "support Option with combined qualifier" in {
      class Foo extends Context { override val bindings = Bindings(bind[String] to "scala",
                                                                   bind[String] to "sindi" as "sindi") }
      val foo = new Foo
      foo.injectAs[Option[String]](qualifier[String] || None) mustEqual Some("scala")
      foo.injectAs[Option[String]]("sindi" || None) mustEqual Some("sindi")
      foo.injectAs[Option[String]]("scala") mustEqual None
    }

    "support Either" in {
      class Foo extends Context { override val bindings = Bindings(bind[String] to "scala",
                                                                   bind[List[String]] to List("sindi")) }
      val foo = new Foo
      foo.inject[Either[List[String], String]] mustEqual Right("scala")
      foo.inject[Either[String, List[String]]] mustEqual Right(List("sindi"))
      foo.inject[Either[String, List[Foo]]] mustEqual Left("scala")
      foo.inject[Either[List[Foo], List[Foo]]] must throwAn[TypeNotBoundException]
    }

    "support combined Either/Option" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      val foo = new Foo
      foo.inject[Option[Either[String, List[String]]]] mustEqual Some(Left("sindi"))
      foo.inject[Option[Either[String, Int]]] mustEqual Some(Left("sindi"))
      foo.inject[Option[Either[Double, Int]]] mustEqual None
      foo.inject[Either[Option[Int], String]] mustEqual Right("sindi")
      foo.inject[Either[String, Option[Int]]] mustEqual Right(None) // Can't evaluate left due to Option on right
    }

    "support filtered injection" in {
      class Foo extends Context { override val bindings = Bindings(bind[String] to "scala",
                                                                   bind[String] to "sindi" as "sindi",
                                                                   bind[List[String]] to List("ioc")) }
      val foo = new Foo
      foo.injectAll[String]("sindi" || None).toList mustEqual List("scala", "sindi")
      foo.injectAll[String]("sindi").toList mustEqual List("sindi")
      foo.injectAll[String]("scala" || "sindi").toList mustEqual List("sindi")
    }

    "autowire class" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      new Foo().autowire[TClass].name mustEqual "sindi"
      new Context{}.autowire[TClass] must throwAn[TypeNotBoundException]
    }

    "autowire case class" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      new Foo().autowire[TCaseClass].name mustEqual "sindi"
      new Context{}.autowire[TCaseClass] must throwAn[TypeNotBoundException]
    }
    "autowire tuple" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      val foo = new Foo
      foo.autowireT[(String, String)] mustEqual ("sindi", "sindi")
      foo.autowireT[(String, Int)] must throwAn[TypeNotBoundException]
    }

    "autowire function" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      val f = (s: String) => "hello " + s
      new Foo().autowire(f).apply mustEqual "hello sindi"
    }
  }

  "Sindi Context with Module" should {

    "autowire imported modules definitions" in {
      class Bar(implicit context: Context) extends Module {
        override val bindings: Bindings = bind[String] to "sindi" 
        def x = inject[String]
      }
      class Foo extends Context { override lazy val modules: Modules = new Bar :: Nil}
      new Foo().autowire[TClass].name mustEqual "sindi" 
    }

    "autowire imported modules definitions by injecting arguments" in {
      class Helper { def f(s: String) = "hello " + s }

      class Bar(implicit context: Context) extends Module {
        override val bindings: Bindings = bind[String] to "sindi"
        def x(h: Helper) = h.f(inject[String])
      }

      class Foo extends Context {
        override lazy val modules: Modules = new Bar :: Nil
        override val bindings: Bindings = bind[Helper] to new Helper
      }

      new Foo().autowire[TClass].name mustEqual "hello sindi" 
    }
  }
}

class TClass(val name: String = "scala")
case class TCaseClass(name: String = "scala")

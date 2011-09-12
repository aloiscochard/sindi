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

// TODO [aloiscochard] Add hierarchical tests

class FunctionalSpec extends Specification {

  "Sindi" should {
    "throw an exception when type is not bound" in {
      class Foo extends Context
      new Foo().inject[String] must throwAn[TypeNotBoundException]
    }

    "bind concrete type" in {
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }
      new Foo().inject[String] mustEqual "sindi"
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

    "bind concrete type with scope" in {
      class Bar
      var state = 1
      class Foo extends Context { override val bindings: Bindings = bind[Bar] to new Bar scope { state } }
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

    "bind parameterized type to provider" in {
      class StringProvider extends Provider[String] { override def provide = "sindi" } 

      class Foo extends Context {
        override val bindings: Bindings = bind[String] toProvider new StringProvider
      }

      val foo = new Foo
      foo.inject[String] mustEqual "sindi"
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
  }
}

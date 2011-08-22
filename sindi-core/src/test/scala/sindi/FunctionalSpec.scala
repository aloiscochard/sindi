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
      class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" as "sindi"}
      val foo = new Foo
      foo.inject[String] must throwAn[TypeNotBoundException]
      foo.injectAs[String]("sindi") mustEqual "sindi"
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

    /*
    "bind parameterized type to provider" in {
      class StringProvider extends Provider[Option[String]] { override def get = Some("sindi") } 

      class Foo extends Context {
        override val bindings: Bindings = bind[Option[String]] toProvider new StringProvider
      }

      val foo = new Foo
      foo.inject[Option[String]] mustEqual Some("sindi")
    }

    "bind parameterized type to abstract provider" in {
      abstract class ListProvider[T <: AnyRef : Manifest] extends Provider[List[T]]

      object ListProvider {
        def create[T <: AnyRef : Manifest] = {
          new ListProvider[T] {
            def provide[T <: AnyRef : Manifest]: T = {
              if (manifest[T].typeArguments.size == 1 && manifest[T].typeArguments.head == manifest[String]) {
                List("sindi")
              } else {
                Nil
              }
            }.asInstanceOf[T]
          }

        }
      } 

      class FooA extends Context {
        override val bindings = Bindings(bind[List[String]] toProvider ListProvider.create[String],
                                         bind[List[AnyRef]] to List("sindi"))
      }
      val fooA = new FooA
      fooA.inject[List[AnyRef]] mustEqual Nil
      fooA.inject[List[String]] mustEqual List("sindi")

      class FooB extends Context {
        lazy val list: List[String] = ListProvider.create[String].provide[List[String]]
        override val bindings = Bindings(bind[List[String]] to list,
                                         bind[List[AnyRef]] to List("sindi"))
      }

      val fooB = new FooB
      fooB.inject[List[AnyRef]] mustEqual List("sindi")
      fooB.inject[List[String]] mustEqual List("sindi")
    }
      */

    "support Option" in {
      class FooA extends Context { override val bindings: Bindings = bind[Option[String]] to Some("sindi") }
      val fooA = new FooA
      fooA.inject[Option[String]] mustEqual Some("sindi")
      fooA.inject[Option[AnyRef]] mustEqual Some("sindi")
      fooA.inject[Option[List[String]]] mustEqual None

      class FooB extends Context 
      new FooB().inject[Option[String]] mustEqual None
    }

  }
}

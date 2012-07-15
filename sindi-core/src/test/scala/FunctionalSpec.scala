//      _____         ___                       
//     / __(_)__  ___/ (_)                      
//    _\ \/ / _ \/ _  / /                       
//   /___/_/_//_/\_,_/_/                        
//                                              
//  (c) 2012, Alois Cochard                     
//                                              
//  http://aloiscochard.github.com/sindi        
//                                              

package sindi.test

import org.specs2.mutable._

import sindi.core._

class FunctionalSpec extends Specification {

  trait Q0

  "Sindi" should {

    "bind and inject" in {
      implicit val string = bind("sindi")
      implicit val int = bind(42)
      inject[String] mustEqual "sindi"
      inject[Int] mustEqual 42
    }

    "support option" in {
      implicit val string = bind("sindi")

      injectOption[String] mustEqual Some("sindi")
      injectOption[Int] mustEqual None

      val f0 = (x: Option[String]) => x
      val f1 = (x: Option[Int]) => x

      autowire(f0) mustEqual Some("sindi")
      autowire(f1) mustEqual None
    }

    "support either" in {
      {
        implicit val string = bind("sindi")
        implicit val int = bind(42)

        injectEither[Int, String] mustEqual Right("sindi")

        val f0 = (x: Either[Int, String]) => x

        autowire(f0) mustEqual Right("sindi")
      }
      {
        implicit val string = bind("sindi")
        injectEither[Int, String] mustEqual Right("sindi")
      }
      {
        implicit val int = bind(42)
        injectEither[Int, String] mustEqual Left(42)
      }
    }

    "support qualifier" in {
      implicit val stringQ0 = bind("sindi", as[Q0])

      val f = (x: String) => x

      as[Q0].inject[String] mustEqual "sindi"
      injectOption[String] mustEqual None
    }
    
    "support qualifiers" in {
      {
        implicit val stringQ0 = bind("sindi", as[Q0])
        implicit val string = bind("")
        (as[Q0], as[Default]).injectOption mustEqual Some("sindi")
        (as[Q0], as[Default]).inject mustEqual "sindi"
      }
      {
        implicit val string = bind("sindi")
        (as[Q0], as[Default]).injectOption mustEqual Some("sindi")
        (as[Q0], as[Default]).inject mustEqual "sindi"
      }
      {
        implicit val stringQ0 = bind("sindi", as[Q0])
        (as[Q0], as[Default]).injectOption mustEqual Some("sindi")
        (as[Q0], as[Default]).inject mustEqual "sindi"
      }
    }
  }
}

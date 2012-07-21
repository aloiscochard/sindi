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
      implicit val stringQ0 = as[Q0].bind("sindi")

      val f = (x: String) => x

      as[Q0].inject[String] mustEqual "sindi"
      injectOption[String] mustEqual None
    }
    
    "support qualifiers" in {
      {
        implicit val stringQ0 = as[Q0].bind("sindi")
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
        implicit val stringQ0 = as[Q0].bind("sindi")
        (as[Q0], as[Default]).injectOption mustEqual Some("sindi")
        (as[Q0], as[Default]).inject mustEqual "sindi"
      }
    }

    "support context" in {
      val context = Context()
      context.bind("sindi")
      context.bind(42)
      context.bind(4.2)

      context.injectAll[Any] must contain("sindi", 42, 4.2)
      context.injectAll[String] mustEqual List("sindi")
      context.injectAll[Int] mustEqual List(42)
      context.injectAll[Double] mustEqual List(4.2)

      import context._
      wire[Seq[String]] mustEqual List("sindi")
      wire[Seq[Int]] mustEqual List(42)
      wire[Seq[Double]] mustEqual List(4.2)
    }

    "support context with qualifier" in {
      val context = Context()

      implicit val string = context.bind("sindi")
      implicit val stringQ0 = context.as[Q0].bind("q0")
      
      context.injectAll[String] must contain("sindi", "q0")
      context.as[Default].injectAll[String] must contain("sindi")
      context.as[Q0].injectAll[String] must contain("q0")

      inject[String] mustEqual "sindi"
    }

    "support operators" in {
      def f(x: String) = x

      implicit val string = <<("sindi")
      implicit val stringQ0 = "q0" :<: as[Q0]

      >>>(f _) mustEqual "sindi"
      f(sindi.core.>>[String]) mustEqual "sindi" // Conflict with specs2

      f(+>) mustEqual "sindi"
      f(as[Q0].+>) mustEqual "q0"
    }

    "support operators in context" in {
      val context = Context()
      context << ("sindi")
      "q0" :<: context.as[Q0]
      
      context.++>[String] must contain("sindi", "q0")
    }
  }
}

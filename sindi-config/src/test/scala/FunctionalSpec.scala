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

import sindi.config._

class FunctionalSpec extends Specification {

  "Sindi Config" should {

    "load simple values" in {
      class Configuration extends DefaultConfiguration("test.conf") {
        val boolean = Key[Boolean]("boolean")
        val double  = Key[Double] ("double")
        val int     = Key[Int]    ("int")
        val long    = Key[Long]   ("long")
        val string  = Key[String] ("string")
      }

      def f(x0: Boolean, x1: Double, x2: Int, x3: Long, x4: String) = (x0, x1, x2, x3, x4)

      val configuration = new Configuration
      import configuration._
      
      implicit val validated = configuration.validate()

      f(boolean, double, int, long, string) mustEqual (true, 2.2, 42, 100000, "sindi") 
    }

    "load sequence values" in {
      class Configuration extends DefaultConfiguration("test.conf") {
        val boolean = Key[List[Boolean]]("list.boolean")
        val double  = Key[List[Double]] ("list.double")
        val int     = Key[List[Int]]    ("list.int")
        val long    = Key[List[Long]]   ("list.long")
        val string  = Key[List[String]] ("list.string")
      }

      def f(x0: List[Boolean], x1: List[Double], x2: List[Int], x3: List[Long], x4: List[String]) = (x0, x1, x2, x3, x4)

      val configuration = new Configuration
      import configuration._

      implicit val validated = configuration.validate()

      f(boolean, double, int, long, string) mustEqual (
        List(true, false, true),
        List(1.1, 2.2, 3.3),
        List(1, 2, 3),
        List(1L, 2L, 3L),
        List("hello", "sindi")
      )
    }

    "support option" in {
      class Configuration extends DefaultConfiguration("test.conf") {
        val boolean = Key[Option[Boolean]]("notexist")
        val string  = Key[Option[String]] ("string")
      }

      val configuration = new Configuration
      import configuration._

      read(boolean) mustEqual Right(None)
      read(string) mustEqual Right(Some("sindi"))
    }

    "support either" in {
      class Configuration extends DefaultConfiguration("test.conf") {
        val e0  = Key[Either[String, Boolean]] ("string")
        val e1  = Key[Either[Int, String]] ("int")
      }

      val configuration = new Configuration
      import configuration._

      read(e0) mustEqual Right(Left("sindi"))
      read(e1) mustEqual Right(Right("42"))
    }

    // TODO Test error, validation, isValid, and config list
  }
}


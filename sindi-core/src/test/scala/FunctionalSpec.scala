//      _____         ___                       
//     / __(_)__  ___/ (_)                      
//    _\ \/ / _ \/ _  / /                       
//   /___/_/_//_/\_,_/_/                        
//                                              
//  (c) 2011, Alois Cochard                     
//                                              
//  http://aloiscochard.github.com/sindi        
//                                              

package sindi.test

import org.specs2.mutable._

import sindi.core._

class ApplicationContext {
  implicit val repository = bind(UserRepository.apply)
  implicit val service = bind(autowire(UserService.apply))
}

class ApplicationContextTesting extends ApplicationContext {
  override implicit val repository = bind[UserRepository](UserRepositoryMock.apply)
}

class UserService(val users: UserRepository)

object UserService { val apply = (users: UserRepository) => new UserService(users) }

class UserRepository
object UserRepository { val apply = new UserRepository }

class UserRepositoryMock extends UserRepository
object UserRepositoryMock { val apply = new UserRepositoryMock }

class FunctionalSpec extends Specification {

  trait Q0

  "Sindi" should {
    "work" in {
      val context = new ApplicationContext
      import context._

      inject[UserRepository]
      inject[UserService]

      def f(x: Option[UserService]) = x

      println(autowire(f _))
      println(as[String].injectOption[UserService])
      true mustEqual true
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
      implicit val string = bind("sindi")
      implicit val int = bind(42)

      injectEither[Int, String] mustEqual Right("sindi")

      val f0 = (x: Either[Int, String]) => x

      autowire(f0) mustEqual Right("sindi")

      /*
      val f0 = (x: Option[String]) => x
      val f1 = (x: Option[Int]) => x

      autowire(f0) mustEqual Some("sindi")
      autowire(f1) mustEqual None
      */
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

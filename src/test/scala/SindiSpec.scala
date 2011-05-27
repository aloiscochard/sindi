package sindi

import org.specs2.mutable._

class Foo

class SindiSpec extends Specification {

  /*
  "Injector Companion" should {
    "create binding from instance" in {
      val foo = new Foo
      val binding = Injector.bind[Foo](foo)
      (binding._1 eq foo.getClass) must beTrue
      (binding._2() eq foo) must beTrue
    }

    "create binding from class" in {
      val binding = Injector.bind[Foo](classOf[Foo])
      val fooA =  binding._2()
      val fooB =  binding._2()
      (binding._1 eq new Foo().getClass) must beTrue
      (fooA ne fooB) must beTrue 
    }

    "create binding from function" in {
      val foo = new Foo
      val function = () => foo
      val binding = Injector.bind[Foo](foo)
      (binding._1 eq foo.getClass) must beTrue
      (binding._2() eq foo) must beTrue
    }
  }
  "Injector" should {
    "accept an input size of 16 bytes only" in {
      new UID((0 to 14).map(_.toByte).toList) must throwAn[IllegalArgumentException]
      new UID((0 to 16).map(_.toByte).toList) must throwAn[IllegalArgumentException]
      new UID((0 to 15).map(_.toByte).toList) must not be null
    }
  }
*/
}

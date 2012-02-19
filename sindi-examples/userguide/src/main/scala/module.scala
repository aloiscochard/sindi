package sindi.examples.userguide.module

import sindi._

object AppContext extends Context {
  import moduleB._
  import moduleA._

  override lazy val modules = new ModuleA :: new ModuleB :: Nil

  /*
  import moduleC._

  override val bindings: Bindings =
    bind[ServiceC] to autowire[CustomServiceC]

    // Disable caching using provider
    //bind[ServiceC] to provider { autowire[CustomServiceC]}

  class CustomServiceC extends ServiceC
  */

  val serviceA = from[ModuleA].service
  val serviceB = from[ModuleB].service

}


package moduleA {
  import moduleC._

  final class ModuleA(implicit context: Context) extends Module {
    override lazy val modules = new ModuleC :: Nil

    override val bindings = Bindings(
      bind[ServiceA] to autowire(new DefaultServiceA(_: ServiceC)),
      bind[ServiceC] to new AdvancedServiceC
    )

    val service = inject[ServiceA]
  }

  trait ServiceA { val c: ServiceC }
  class DefaultServiceA(override val c: ServiceC) extends ServiceA
}

package moduleB {
  import moduleC._

  final class ModuleB(implicit context: Context) extends Module {
    override lazy val modules = new ModuleC :: Nil

    override val bindings: Bindings =
      bind[ServiceB] to autowire(new DefaultServiceB(_: ServiceC))

      // Without autowiring:
      //bind[ServiceB] to new DefaultServiceB(from[ModuleC].service)

    val service = inject[ServiceB]
  }

  trait ServiceB { val c: ServiceC }
  class DefaultServiceB(override val c: ServiceC) extends ServiceB

}

package moduleC {
  final class ModuleC(implicit context: Context) extends Module {
    override val bindings: Bindings =
      bind[ServiceC] to new DefaultServiceC

    val service = inject[ServiceC]
  }

  trait ServiceC
  class DefaultServiceC extends ServiceC
  class AdvancedServiceC extends ServiceC
}

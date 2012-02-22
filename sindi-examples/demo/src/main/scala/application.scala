package sindi.examples
package application

package app {
  import sindi._

  final class AppContext(production: Boolean = false) extends Context {
    import core.module.Core
    import framework.logging

    override lazy val modules = new Core(this) :: Nil

    override val bindings: Bindings = if (production) {
      bind[logging.api.Formatter] to new logging.impl.SimpleFormatterWithDate
    } else {
      Nil
    }

    def start = from[Core].start
  }
}

package core {
  import framework.logging.api._

  trait ServiceA extends Logging {
    def start = log.info("ServiceA")
  }

  trait ServiceB extends Logging {
    def start = log.info("ServiceB")
  }

  package module {
    import sindi._
    import framework.logging.module._

    final class Core(override val ctx: Context) extends Module {
      override lazy val modules = new Logging(this) :: Nil

      override val bindings = Bindings(
        bind[ServiceA] to new ComponentContext with ServiceA with LoggingComponent,
        bind[ServiceB] to new ComponentContext with ServiceB with LoggingComponent
      )

      def start = {
        inject[ServiceA].start
        inject[ServiceB].start
      }
    }
  }
}

package sindi.examples.framework.logging

package api {
  trait Logging { val log: Logger }

  trait Logger { def info(message: String): Unit }

  trait Formatter {
    def format(message: String): String
  }
}

package impl {
  class SimpleLogger(formatter: api.Formatter) extends api.Logger {
    def info(message: String): Unit = println(formatter.format(message))
  }

  class SimpleFormatter extends api.Formatter {
    def format(message: String) = "info: " + message
  }

  class SimpleFormatterWithDate extends api.Formatter {
    def format(message: String) = new java.util.Date + " - info: " + message
  }
}

package module {
  import sindi._

  final class Logging(override val ctx: Context) extends Module {
    override val bindings = Bindings(
      bind[api.Logger] to new impl.SimpleLogger(inject[api.Formatter]),
      bind[api.Formatter] to new impl.SimpleFormatter
    )

    def logger = inject[api.Logger]
  }

  trait LoggingComponent extends Component with api.Logging { val log = from[Logging].logger }
}

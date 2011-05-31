package sindi.examples.qualifier

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  import consumer._
  import service._
  val consumerModule = ConsumerModule(this)
  define {
    bind[Service] to new AdvancedService as "consumer"
  }
  consumerModule[Consumer].consume()
}

package consumer {
  import sindi.examples.qualifier.service._

  object ConsumerModule {
    def apply(implicit context: Context) = new Module {
      val serviceModule = ServiceModule(this)
      define {
        bind[Consumer] to new Consumer(serviceModule[Service]("consumer"))
        bind[Service] to serviceModule[Service] as "consumer"
      }
    }
  }
  

  class Consumer(service: Service) {
    def consume() = println(service)
  }
}

package service {

  object ServiceModule {
    def apply(implicit context: Context) = new Module { define { bind[Service] to new DefaultService scope singleton } }
  }

  trait Service {
    def name
  }

  class DefaultService extends Service { def name = "default" }
  class AdvancedService extends Service { def name = "advanced" }

}

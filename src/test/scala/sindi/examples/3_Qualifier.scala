package sindi.examples.qualifier

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  val o = consumer.Consumer(this)
  val s = service.Service(this)
  define {
    bind[service.Service] to s() as "consumer"
  }
  o().consume()
}

package consumer {
  import sindi.examples.qualifier.service._

  object Consumer {
    def apply(context: Context) = new ConsumerComponent(context)
  }

  class ConsumerComponent(context: Context) extends Component[Consumer](context: Context) {
    val service = Service(this)
    define {
      bind[Consumer] to new Consumer(service("consumer"))
      //bind[Service] to service() as "consumer"
    }
  }
  

  class Consumer(service: Service) {
    def consume() = println(service.name)
  }
}

package service {

  object Service {
    def apply(context: Context) = new ServiceComponent(context)
  }

  class ServiceComponent(context: Context) extends Component[Service](context: Context) {
    define {
      bind[Service] to new DefaultService scope singleton
    }
  }

  trait Service {
    def name
  }

  class DefaultService extends Service { def name = "default" }
  class AdvancedService extends Service { def name = "advanced" }

}

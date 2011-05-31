package sindi.examples.qualifier

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  import consumer._
  import service._
  val consumerComponent = Consumer(this)
  val serviceComponent = Service(this)
  define {
    bind[Service] to new AdvancedService as "consumer"
  }
  consumerComponent[Consumer]().consume()
}

package consumer {
  import sindi.examples.qualifier.service._

  object Consumer {
    def apply(context: Context) = new ConsumerComponent()(context)
  }

  class ConsumerComponent(implicit context: Context) extends Component {
    val serviceComponent = Service(this)
    define {
      bind[Consumer] to new Consumer(serviceComponent[Service]("consumer"))
      //bind[Service] to service() as "consumer"
    }
  }
  

  class Consumer(service: Service) {
    def consume() = println(service)
  }
}

package service {

  object Service {
    def apply(context: Context) = new ServiceComponent()(context)
  }

  class ServiceComponent(implicit context: Context) extends Component {
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

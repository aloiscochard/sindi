package sindi.examples.qualifier

/*
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

  object ConsumerModule extends ModuleFactory[ConsumerModule]

  class ConsumerModule(implicit val context: Context) extends Module { 
      private val serviceModule = ServiceModule(this)

      define {
        bind[Consumer] to new Consumer(serviceModule[Service]("consumer"))
        bind[Service] to serviceModule[Service] as "consumer"
      }
  }
  
  class Consumer(service: Service) {
    def consume() = println(service)
  }
}

package service {

  object ServiceModule extends ModuleFactory[ServiceModule] 

  class ServiceModule(implicit val context: Context) extends Module {
    define {
      bind[Service] to new DefaultService scope singleton
    } 
  }

  trait Service 
  class DefaultService extends Service
  class AdvancedService extends Service
}
*/

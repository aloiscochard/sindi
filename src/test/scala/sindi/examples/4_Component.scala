package sindi.examples.component

import sindi._

/////////////////
// Application //
/////////////////

object Application extends App with Context {
  import service._
  ServiceModule.childify(this)
  define {
    bind[Service] to new AdvancedService
  }
  new Consumer
}

class Consumer extends service.ServiceComponent {
  println(this.service)
}

package service {

  object ServiceModule extends Environment {
    def configure = () => {
      bind[Service] to new DefaultService scope singleton
    }
  }

  trait ServiceComponent extends Component {
    override protected lazy val injector = ServiceModule.injector
    lazy val service = injector.inject[Service]
  }

  trait Service {
    def name
  }

  class DefaultService extends Service { def name = "default" }
  class AdvancedService extends Service { def name = "advanced" }

}

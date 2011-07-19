//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi
                    
// TODO [aloiscochard] Get ride of ScalaBeans by doing reflection by hand
// TODO [aloiscochard] Add qualifier in type not bound exception
// TODO [aloiscochard] Add provided binding to handle mapper binding automatically thru available class
// TODO [aloiscochard] Cache childable context using actor (try to use actor for shared state everywhere)

// TODO [aloiscochard] Add assertion and error message
// TODO [aloiscochard] Add assertion check on context.bindings when locked

// SINAP
// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Implement Event/Lifecycle system

object Sindi extends context.Context with context.Configurable

trait Context extends context.Context with context.Childifiable with context.Configurable {
  private var modules = Map[Class[_ <: Module], sindi.injector.Injector]()

  def from[M <: Module : Manifest] = modules(manifest[M].erasure.asInstanceOf[Class[M]])

  override protected def default = () => sindi.injector.Injector(bindings, () => Sindi.injector)

  protected def include(_modules: Module*) = {
    for (module <- _modules) { modules += module.getClass.asInstanceOf[Class[_ <: Module]] -> module.injector }
  }
}

abstract class Module(implicit context: Context) extends Context {
  childify(context)
  def apply[S <: AnyRef : Manifest](): S = inject[S]
  def apply[S <: AnyRef : Manifest](qualifier: AnyRef): S = injectAs[S](qualifier)
}

abstract class ModuleFactory[M <: Module : Manifest] {
  def apply(implicit context: Context): M = {
    (manifest[M].erasure.getConstructor(classOf[Context]).newInstance(context)).asInstanceOf[M]
  }
}

trait Component { 
  protected def from[M <: Module : Manifest]: injector.Injector
}

class ComponentContext(val context: Context) extends Component {
  protected def from[M <: Module : Manifest] = context.from[M]
}

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

//object Sindi extends context.Context with context.Configurable

trait Context extends context.Context with context.Childifiable with context.Configurable {
  protected val modules: List[Module] = Nil

  def from[M <: Module : Manifest]: sindi.injector.Injector = {
    modules.foreach((m: Module) => {
      if (m.getClass == manifest[M].erasure.asInstanceOf[Class[M]]) return m.asInstanceOf[M].injector
    })
    // TODO [aloiscochard] Remove this ugly hack and put a monad instead
    new RuntimeException("Unable to inject from module %s: module is not found".format(manifest[M].erasure))
    "".asInstanceOf[M].injector
  }

  override protected def default = () => sindi.injector.Injector(bindings)
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

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
                    
// TODO [aloiscochard] Add pre/post processor
// TODO [aloiscochard] Add provided binding to handle mapper binding automatically thru available class
// TODO [aloiscochard] Add assertion and error message

// SINAP
// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Implement Event/Lifecycle system

trait Context extends context.Context with binder.Binder {
  protected val modules: List[Module] = Nil

  def from[M <: Module : Manifest]: sindi.injector.Injector = {
    modules.foreach((m: Module) => {
      if (m.getClass == manifest[M].erasure.asInstanceOf[Class[M]]) return m.asInstanceOf[M].injector
    })
    throw new RuntimeException("Unable to inject from module %s: module not found.".format(manifest[M].erasure.getName))
  }
}

abstract class Module(implicit context: Context) extends Context with context.Childified {
  override protected val parent = context
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

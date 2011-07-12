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
                    
// TODO [aloiscochard] Add qualifier in type not bound exception
// TODO [aloiscochard] Add provided binding to handle mapper binding automatically thru available class
// TODO [aloiscochard] Cache childable context using actor (try to use actor for shared state everywhere)

// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Add assertion and error message
// TODO [aloiscochard] Add assertion check on context.bindings when locked

object Sindi extends context.Context with context.Configurable

trait Context extends context.Context with context.Childifiable with context.Configurable {
  override protected def default = () => sindi.injector.Injector(bindings, () => Sindi.injector)
}

abstract class Module(implicit context: Context) extends Context {
  childify(context)
  def apply[S <: AnyRef : Manifest](): S = inject[S]
  def apply[S <: AnyRef : Manifest](qualifier: AnyRef): S = injectAs[S](qualifier)
}

trait ModuleFactory extends binder.Configurable {
  def apply(implicit context: Context) = create(context) 

  def define(configure: => Unit) = { configure }

  protected def create(implicit context: Context) = new Module { bindings = bindings ++ elements.map(e => e.build) }
}

trait StaticContext extends binder.Configurable { 
  lazy val injector = context.injector

  def childify(_context: Context) = context.childify(_context)

  private lazy val context = {
    val _elements = elements
    new Context { bindings = bindings ++ _elements.map(e => e.build) }
  }
}

trait Component extends injector.Delegatable { override protected lazy val injector = Sindi.injector }

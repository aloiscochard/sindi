package sindi

// TODO [aloiscochard] Add qualifier in type not bound exception
// TODO [aloiscochard] Add provided binding to handle mapper binding automatically thru available class

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

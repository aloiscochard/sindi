package sindi

// TODO [aloiscochard] qualifier fallback: test use of ||
// TODO [aloiscochard] @default annotation for defImpl or other strategy ?
// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Add assertion and error message
// TODO [aloiscochard] Add assertion check on context.bindings when locked

object Sindi extends context.Context with context.Configurable

trait Context extends context.Context with context.Childifiable with context.Configurable {
  override protected def default = () => sindi.injector.Injector(bindings, () => Sindi.injector)
}

abstract class Component(implicit context: Context) extends Context {
  childify(context)
  def apply[S <: AnyRef : Manifest](): S = inject[S]
  def apply[S <: AnyRef : Manifest](qualifier: AnyRef): S = injectAs[S](qualifier)
}

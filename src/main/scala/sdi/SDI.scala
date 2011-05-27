package sdi

// TODO [aloiscochard] qualifier fallback: test use of ||
// TODO [aloiscochard] @default annotation for defImpl or other strategy ?
// TODO [aloiscochard] map to config[file]
// TODO [aloiscochard] Add assertion and error message
// TODO [aloiscochard] Add assertion check on context.bindings when locked

class inject extends injector.inject

object SDI extends context.Context with context.Configurable

trait Context extends context.Context with context.Childifiable with context.Configurable {
  override protected def default = () => sdi.injector.Injector(bindings, () => SDI.injector)
}


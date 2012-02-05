package sindi.examples.userguide.provider

import sindi._

class Foo
class Bar

object AppContext extends Context {
  override val bindings = Bindings(
    bind[Foo] to new Foo,
    bind[Bar] to provider { new Bar }
  )
}

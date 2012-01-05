Context
=======

Generally a `context <http://aloiscochard.github.com/sindi/api/index.html#sindi.Context>`_
describe how a set of compounds should be wired together,
once instantiated during runtime it provide facilities to inject theses compounds.

There is multiple way to describe a context and his bindings,
but it's **highly recommended** to use the provided DSL.

*By using the DSL,
you allow the compiler plugin to recognize your declaration and check them during compilation.*

Let's take a look at this very simple context definition::

  class Foo extends Context { override val bindings: Bindings = bind[String] to "sindi" }

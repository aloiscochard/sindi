Context
=======

Generally a `context <http://aloiscochard.github.com/sindi/api/index.html#sindi.Context>`_
describe how a set of compounds should be wired together,
once instantiated during runtime it provide facilities to inject theses compounds.

There is multiple way to describe a context and his bindings,
but it's **highly recommended** to use the provided DSL.

*By using the DSL, you allow the compiler plugin to recognize and check your declaration/injection during compilation.*

Let's take a look at this very simple context definition::

  import sindi._

  object AppContext extends Context {
    override val bindings: Bindings = bind[String] to "sindi"
    val name = inject[String]
  }

We first override the ``bindings`` value and use the DSL to *bind* the type ``String`` to the value ``"sindi"``,
then we declare a public value ``name`` which *inject* the type ``String``.

And now let's see how we can consume the ``AppContext`` object::

  scala> AppContext.name
  res0: String = sindi

In order for **injections to be checked by the compiler plugin** they should be done inside the underlying context.

The usage of *qualifiers* make possible to differentiate bindings of same type::

  object AppContext extends Context {
    trait Name
    trait Author

    override val bindings = Bindings(
      bind[String] to "sindi" as qualifier[Name],
      bind[String] to "Alois Cochard" as qualifier[Author]
    )

    val name = injectAs[String](qualifier[Name])
    val author = injectAs[String](qualifier[Author])
  }

In order for **qualified bindings to be checked by the compiler plugin** the usage of dynamic value
(as in ``bind[String] to "sindi" as "name"``) should be avoided.

Before moving into next topic and diving into modularization, let's see how we can automate injection using *autowiring*::
  
  import sindi._
  import java.net.URI

  object Formatter {
    def name(n: String) = "Name: " + n
    def website(u: URI) = "Website: " + u
  }

  case class Info(name: String, website: URI)

  object AppContext extends Context {
    import Formatter._

    override val bindings = Bindings(
      bind[String] to "sindi",
      bind[URI] to new URI("http://aloiscochard.github.com/sindi/")
    )

    val info = autowire[Info]

    val infoT = autowireT[(String, URI)]

    val description = List(autowire(name _), autowire(website _))
                        .map(_.apply).mkString(", ")
  }

Autowiring provide constructors, tuple and parameters injection::

  scala> AppContext.info
  res0: AppContext.Info = Info(sindi,http://aloiscochard.github.com/sindi/)

  scala> AppContext.infoT
  res1: (String, java.net.URI) = (sindi,http://aloiscochard.github.com/sindi/)

  scala> AppContext.description
  res2: String = Name: sindi, Website: http://aloiscochard.github.com/sindi/

Implementation isn't currently complete, the following features are missing:
 * Compiler plugin checking
 * Support of TupleX and FunctionX

**Theses features are under development and will be integrated in next version (0.5).**

The examples you've just seen demonstrate internal context usage but wouldn't make lot of sense in real world situations,
we could simply have used global object (``val``) to hold the shared values.

Using an IoC container make sense in situation when multiple modules that share common dependencies should be integrated together,
so it's probably time to create your first :doc:`module` to understand how to design with Sindi.


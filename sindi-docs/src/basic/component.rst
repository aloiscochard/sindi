Component
=========

A `component <http://aloiscochard.github.com/sindi/api/index.html#sindi.Component>`_
is basically a set of features based on module(s) service(s).

It provide a flexible alternative to the
`cake-pattern <http://jboner.github.com/2008/10/06/real-world-scala-dependency-injection-di.html>`_ 
popular in the Scala universe,
using components you can organize features from one or multiple modules,
or use it as alternative to construtor parameters injection.

To see all possibilites offered by components, let's first define two simple services::

  trait UserRepository
  class DefaultUserRepository extends UserRepository
  class AdvancedUserRepository extends UserRepository

  trait UserService {
    def repository: UserRepository
    def name: String                                                  
  }                                                                        
  trait DefaultUserService extends UserService  { def name = "default" }  
  trait AdvancedUserService extends UserService  { def name = "advanced" }

The first is ``UserRepository`` which is used by the second one ``UserService``.
We can now create a module for ``UserRepository``::

  final class UserRepositoryModule(override val ctx: Context) extends Module {
    override val bindings: Bindings =
      bind[UserRepository] to new DefaultUserRepository

    def repository = inject[UserRepository]
  }

To ease the integration of ``UserRepository`` we can now create our first component::

  trait UserRepositoryComponent extends Component {       
    def repository = from[UserRepositoryModule].repository
  }

As you can see, to define a new Component, you simply create a *trait* extending the trait ``Component``,
you can now expose any modules feature by using the ``from[Module].feature`` syntax.

It's one more time **highly recommend** to use the compiler plugin: 
a component is an abstract set of features and the compiler plugin need to know which modules
are used for a given components,
this is done automatically if you are using the compiler plugin,
otherwise you shoud define constrains manually using ``ModuleManifest``::

  trait UserRepositoryComponent extends Component { 
    private def userRepositoryManifest = new ModuleManifest[UserRepositoryModule]
    def repository = from[UserRepositoryModule].repository
  } 
 
Let's move forward and see how we can consume the ``UserRepositoryComponent`` by creating a module for ``UserService``::

  final class UserServiceModule(override val ctx: Context) extends Module {
    override lazy val modules = new UserRepositoryModule(this) :: Nil            
                                                                           
    override val bindings: Bindings =                           
      bind[UserService] to           
        new ComponentContext with UserRepositoryComponent with DefaultUserService 
                                                                                  
    def service = inject[UserService]                                             
  }                                  
                                     

First we import the ``UserRepositoryModule``, and then we bind ``UserService`` to a new 
`ComponentContext <http://aloiscochard.github.com/sindi/api/index.html#sindi.ComponentContext>`_
which enable us to mixin component(s) on it and get them automatically linked within the current context!

In case your mixin component that requiere modules which are **not imported** in the current context,
the **compiler plugin will generate an error** preventing experiencing unbound dependencies at runtime.

This can give the feeling of lot of boilerplate for little advantage, in fact components start really to shine
when you want to combine features from a complex module or aggregate feature from different modules.

Component have an other very useful role when you start integrating Sindi into your application container,
if your container (specially some webframeworks) requiere you to create global ``object`` you can use 
`ComponentWith <http://aloiscochard.github.com/sindi/api/index.html#sindi.ComponentWith>`_.

Let's see a concrete use case by first adding a component for ``UserService``::

  trait UserServiceComponent extends Component {
    def userService = from[UserServiceModule].service
  } 

And now create the application context::

  object AppContext extends Context {
    override lazy val modules = new UserServiceModule(this) :: Nil
  }

Finally we create a component who is statically linked to our application context::

  trait AppComponent extends ComponentWith[AppContext.type] with UserServiceComponent {
    override val context = AppContext                                                  
  }                                                              

By defining our component with ``ComponentWith`` we specify to which context he's linked using the type parameter
(enabling compiler plugin to check if dependencies are bound) and then pass the concrete ``AppContext``
by overriding the ``context`` value.


We can now use this component on any ``object`` and get dependency injected from the ``AppContext``::

  scala> object Test extends AppComponent
  defined module Test

  scala> Test.userService.name
  res0: String = default

Here finish your journay into Sindi's basics features, if you feel brave enough you can continue
to the :doc:`/advanced` documentation!



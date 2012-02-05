Provider
========

By default bindings are created a *cached* provider,
you can change this behavior by using a standard provider::
  class Foo                            
  class Bar                            
                                       
  object AppContext extends Context {  
    override val bindings = Bindings(  
      bind[Foo] to new Foo,            
      bind[Bar] to provider { new Bar }
    )                                  
  }                                    

Let's see it in action::

  scala> AppContext.inject[Foo]
  res0: Foo = Foo@6beb3926

  scala> AppContext.inject[Foo]
  res1: Foo = Foo@6beb3926

  scala> AppContext.inject[Bar]
  res2: Bar = Bar@6e1513f3

  scala> AppContext.inject[Bar]
  res3: Bar = Bar@1e2db6ea


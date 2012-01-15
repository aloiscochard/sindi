Module
======

A `module <http://aloiscochard.github.com/sindi/api/index.html#sindi.Module>`_
provide services based on a configuration which can be overrided by the module container.

Let's take a look at this simple modularization use case (arrows indicate "depends on"):
             
.. image:: /images/basic_module_01.png

When designing application with an IoC container, you want to avoid coupling as much as possible.
For instance, ModuleC could be self-contained in a jar file and he doesn't need other modules to be used.

          

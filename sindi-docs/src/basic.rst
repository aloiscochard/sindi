Introduction
============

Welcome to the Sindi IoC Container official documentation!

This guide will explain you what you can achieve with Sindi and how to do so,
but it won't explain you why/how using IoC to achieve a well crafted modular architecture.

If your looking for an introduction to IoC, here is some resources that might be of interest to you:
 * `Inversion of Control <http://martinfowler.com/bliki/InversionOfControl.html>`_
 * `IoC and the DI pattern <http://martinfowler.com/articles/injection.html>`_

If you already know an other IoC/DI framework,
you should keep in mind that Sindi take a different approach than others traditionals frameworks.

In this regards please take a look at the wiki page
"`How does Sindi compare with X? <http://github.com/aloiscochard/sindi/wiki/Comparison>`_"
before continuing your reading here.

The framework is composed of two main artifacts:
 * sindi-core: A library providing IoC functionalities
 * sindi-compiler: A compiler plugin that resolve and check dependencies during compilation

The compiler plugin is totally **optional**,
you can use Sindi without it but you'll miss some very helpful feature and unfortunatly,
you take the risk of having your **application crashing at runtime due to missing dependencies**.

*Every features who are specific to the usage of the compiler plugin
are clearly mentioned as such in this guide.*

It can be handy to have a small project to experiment with Sindi while reading this documentation, give a look at the wiki page
"`Download <http://github.com/aloiscochard/sindi/wiki/Download>`_"
to integrate Sindi in a SBT or Maven project.

You can find a ready to use project with all examples used in this guide
`here <http://github.com/aloiscochard/sindi/wiki/Examples>`_.

And now let's familiarize with Sindi's core concepts!

.. toctree::
    :maxdepth: 2

    basic/context
    basic/module
    basic/component

# Scala Dependency Injector

*WARNING ALPHA VERSION*

## Getting Started

Using the SDI root context directly:

    import sdi.SDI._

    class Component
    class UID

    define {
      val c = new Component
      bind[Component] to c
      bind[UID] to new UID
    }

    def intro() {
        for (x <- 1 to 4) {
          println(inject[Component])
          println(inject[UID])
        }
    }

    // REPL
    scala> sdi.exemples.introduction.Introduction.intro
    sdi.exemples.introduction.Introduction$Component@7646dfb1
    sdi.exemples.introduction.Introduction$UID@27081a21
    sdi.exemples.introduction.Introduction$Component@7646dfb1
    sdi.exemples.introduction.Introduction$UID@4eb808c
    sdi.exemples.introduction.Introduction$Component@7646dfb1
    sdi.exemples.introduction.Introduction$UID@14469da
    sdi.exemples.introduction.Introduction$Component@7646dfb1
    sdi.exemples.introduction.Introduction$UID@328167dc

Some functional fun:

    object Functional extends sdi.Context {
      define {
        var i = 1
        bind[String] to { i = i + i; "i=" + i }
      }

      def haveFun = {
        for (x <- 1 to 4) {
          println(inject[String])
        }
      }
    }

    // REPL
    scala> sdi.exemples.introduction.Introduction.Functional.haveFun
    i=2
    i=4
    i=8
    i=16

You can find full examples in the source:

  * [Basic](http://github.com/aloiscochard/sdi/blob/master/src/test/scala/sdi/examples/1_Basic.scala)
  * [Annotation](http://github.com/aloiscochard/sdi/blob/master/src/test/scala/sdi/examples/2_Annotation.scala)

## Download

# SBT

      val sdi = "com.github.aloiscochard.sdi" %% "sdi" % "0.1-SNAPSHOT"

      val aloiscochardSnapshots = "aloiscochard snapshots" at "http://orexio.org/~alois/repositories/snapshots" 

# Maven

    <dependencies>
      ...
      <dependency>
        <groupId>com.github.aloiscochard.sdi</groupId>
        <artifactId>sdi_2.9.0</artifactId>
        <version>0.1-SNAPSHOT</version>
      </dependency>
      ...
    </dependencies>

    <repositories>
      ...
      <repository>
        <id>aloiscochard snapshots</id>
        <url>http://orexio.org/~alois/repositories/snapshots</url>
      </repository>
      ...
    </repositories>

## Credits
[Alois Cochard](http://aloiscochard.blogspot.com)

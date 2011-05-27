# Sindi
[http://aloiscochard.github.com/sindi](http://aloiscochard.github.com/sindi)

Sindi is an IoC ([Inversion of Control](http://martinfowler.com/articles/injection.html)) container for the [Scala](http://www.scala-lang.org) programming language.

## Examples

  * [Introduction](http://github.com/aloiscochard/sindi/blob/master/src/test/scala/sindi/examples/0_Introduction.scala)
  * [Idiomatic](http://github.com/aloiscochard/sindi/blob/master/src/test/scala/sindi/examples/1_Basic.scala) (Service Locator Pattern)
  * [Annotation](http://github.com/aloiscochard/sindi/blob/master/src/test/scala/sindi/examples/2_Annotation.scala) (Dependency Injection Pattern)

## Download

# SBT

      val sindi = "org.scala-tools.sindi" %% "sindi" % "0.1-SNAPSHOT"

      val aloiscochardSnapshots = "aloiscochard snapshots" at "http://orexio.org/~alois/repositories/snapshots" 

# Maven

    <dependencies>
      ...
      <dependency>
        <groupId>org.scala-tools.sindi</groupId>
        <artifactId>sindi_2.9.0</artifactId>
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

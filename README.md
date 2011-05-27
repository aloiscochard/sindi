# Sindi

Sindi is an IoC ([Inversion of Control](http://martinfowler.com/articles/injection.html)) container for the [Scala](http://www.scala-lang.org) programming language.

User Guide: [http://aloiscochard.github.com/sindi](http://aloiscochard.github.com/sindi)
Mailling List: [http://aloiscochard.github.com/sindi](http://aloiscochard.github.com/sindi)

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

## License

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2011 Shay Banon and ElasticSearch <http://www.elasticsearch.com>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

## Credits
*Founder*
* [Alois Cochard](http://aloiscochard.blogspot.com)

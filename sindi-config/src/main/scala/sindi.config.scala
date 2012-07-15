//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2012, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi

import scala.collection.JavaConverters._
import scala.util.control.Exception._

import java.io.File
import java.net.URL

import com.typesafe.config._

import core._

package object config {

  trait Configurable {
    import sindi.core._

    implicit val profile = bind(Option(System.getProperty("profile")).orElse(defaultProfile), as[Profile])

    class Configuration(implicit binding: Binding[Option[String], Profile]) 
      extends config.DefaultConfiguration("application" + binding.inject.map("-" + _).getOrElse("") + ".conf")

    trait Profile

    protected val defaultProfile: Option[String] = None
  }

  trait Configuration {
    implicit def key2value[T : Reader](key: Key[T]) = read(key)

    // TODO Add implicit from regular expression to validation
    object Key {
      def apply[T : Reader](name: String) = apply[T](name, (_: T) => Nil)
      def apply[T : Reader](name: String, validation: T => List[String]) =
        Configuration.this.validate(new Key[T](name, validation))
    }

    def config = _config.toList
    def errors = _errors.toList
    def isValid = _errors.isEmpty

    def read[T](key: Key[T])(implicit reader: Reader[T]) = reader(key)
    def validate[T : Reader](key: Key[T]): Key[T] = {
      val value = read(key) // TODO Catch exception
      _config += key.name -> value.toString
      key.validation(value) match {
        case Nil =>
        case xs => _errors += key.name -> xs
      }
      key
    }

    private var _config = Map[String, String]()
    private var _errors = Map[String, List[String]]()
  }

  trait Reader[T] {
    def apply(key: Key[T]): T
  }

  object Reader {
    def apply[T](f: Key[T] => T) = new Reader[T] { def apply(key: Key[T]): T = f(key) }

  }

  class Key[T](val name: String, val validation: T => List[String])

  class DefaultConfiguration(config: Config) extends Configuration {

    def this(resourceName: String) = this(ConfigFactory.load(resourceName))
    def this(file: File) = this(ConfigFactory.parseFile(file))

    // TODO Add URL support
    // TODO Find a fix to avoid exception when Missing/Wrong format
    // Would probably need to do process Key in 1step, first validation, then can get data directly if valid (thru implicit?)

    implicit def _option[T](implicit reader: Reader[T]): Reader[Option[T]] =
      Reader(key => catching(classOf[ConfigException.Missing]).opt(read(Key[T](key.name))))


    implicit val _boolean = reader(config.getBoolean(_))
    implicit val _double = reader(config.getDouble(_))
    implicit val _int = reader(config.getInt(_))
    implicit val _long = reader(config.getLong(_))
    implicit val _string = reader(config.getString(_))

    implicit val _booleanL = reader(config.getBooleanList(_).asScala.toList.map(x => x: Boolean))
    implicit val _doubleL = reader(config.getDoubleList(_).asScala.toList.map(x => x: Double))
    implicit val _intL = reader(config.getIntList(_).asScala.toList.map(x => x: Int))
    implicit val _longL = reader(config.getLongList(_).asScala.toList.map(x => x: Long))
    implicit val _stringL = reader(config.getStringList(_).asScala.toList)

    private def reader[T](f: String => T) = Reader[T](key => f(key.name))
  }
}

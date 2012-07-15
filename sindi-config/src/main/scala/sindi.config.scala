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

package object config {

  trait Configuration {
    implicit def key2value[T : Reader](key: Key[T]) = read(key) match {
      case Right(value) => value
      case Left(error) => throw new Exception("Configuration error for key '%s': %s".format(key.name, error))
    }

    // TODO Add implicit for URL support (from String)
    // TODO Add implicit from regular expression to validation
    implicit def _option[T](implicit reader: Reader[T]): Reader[Option[T]] = Reader(key => read(Key[T](key.name)) match {
      case Right(value) => Right(Some(value))
      case Left(Missing) => Right(None)
      case Left(error) => Left(error)
    })

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
      val value = read(key)
      value match {
        case Right(value) => {
          _config += key.name -> value.toString
          key.validation(value) match {
            case Nil =>
            case xs => _errors += key.name -> xs
          }
        }
        case Left(error) => _errors += key.name -> List(error.toString)
      }
      key
    }

    private var _config = Map[String, String]()
    private var _errors = Map[String, List[String]]()
  }

  sealed trait ConfigurationError
  case object Missing extends ConfigurationError 
  case object WrongType extends ConfigurationError 

  class Key[T](val name: String, val validation: T => List[String])

  trait Reader[T] { def apply(key: Key[T]): Either[ConfigurationError, T] }

  object Reader {
    def apply[T](f: Key[T] => Either[ConfigurationError, T]) = new Reader[T] { def apply(key: Key[T]) = f(key) }
  }

  class DefaultConfiguration(config: Config) extends Configuration {

    def this(resourceName: String) = this(ConfigFactory.load(resourceName))
    def this(file: File) = this(ConfigFactory.parseFile(file))

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

    private def reader[T](f: String => T) = Reader[T](key => catching(classOf[ConfigException]) either f(key.name) match {
      case Right(value) => Right(value)
      case Left(x) => x match {
        case x: ConfigException.Missing => Left(Missing)
        case x: ConfigException.WrongType => Left(WrongType)
      }
    })
  }
}

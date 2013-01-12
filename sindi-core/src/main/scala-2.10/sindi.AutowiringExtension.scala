//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2013, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi

import scala.reflect.macros.Context
import scala.reflect.runtime.universe.TypeTag
import scala.tools.reflect.Eval

trait AutowiringExtension {
  import language.experimental.macros
  def autowire[T](implicit tag: TypeTag[T]) = macro AutowiringMacro.autowire[T]
  def <<<[T](implicit tag: TypeTag[T]) = macro AutowiringMacro.autowire[T]
}

object AutowiringMacro {
  def autowire[T](c: Context)(tag: c.Expr[TypeTag[T]]) = {
    import c.universe._
    val TypeRef(_, _, tpe :: Nil) = tag.actualType

    val constructor = tpe.declarations.toList.collect {
      case m: MethodSymbol if m.isConstructor => m
    }.sortBy(_.paramss.flatten.size).last

    val paramss = constructor.paramss.map(_.map { param =>
      List(
        appliedType(typeOf[sindi.Binding[_, _]], List(param.typeSignature, tpe)),
        appliedType(typeOf[sindi.Wire[_]], List(param.typeSignature))
      ).foldLeft(EmptyTree)((binding, tpe) => binding match {
        case EmptyTree => c.inferImplicitValue(tpe)
        case tree => tree
      }) match {
        case EmptyTree => c.abort(c.enclosingPosition, s"could not bind parameter ${param.name}: ${param.typeSignature}")
        case tree => Apply(Select(tree, newTermName("apply")), List())
      }
    })

    def cons(paramss: List[List[Tree]]): Tree = paramss match {
      case head :: Nil => Apply(Select(New(Ident(tpe.typeSymbol)), nme.CONSTRUCTOR), head)
      case head :: tail => Apply(cons(tail), head)
    }

    c.Expr[T](cons(paramss.reverse))
  }
}

//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi.compiler
package model 

import scala.actors.Actor
import scala.collection.immutable.HashMap

import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin
import nsc.util.SourceFile

abstract class ModelPlugin(val global: Global) extends Plugin {
  import global._

  var options: Options

  // TODO Replace unit with static field like sourcename
  case class CompilationUnitInfo(source: SourceFile, contexts: List[Context], components: List[Component])

  case class Context(tree: Tree, modules: List[Type], bindings: List[Binding], dependencies: List[Dependency]) extends Entity {
    override def toString = super.toString + {
      if (!modules.isEmpty) " [ modules: " + modules.mkString(", ") + " ]"
    }

  }

  case class Component(tree: Tree, module: Option[Symbol], dependencies: List[Dependency]) extends Entity {
    override def toString = "[" + module.map(_.name).getOrElse("undefined") + "] " + super.toString

  }

  private[model] trait Entity {
    def tree: Tree 
    def dependencies: List[Dependency]
    override def toString = tree.symbol.name.toString + {
      if (!dependencies.isEmpty) " { dependencies: " + dependencies.mkString(", ") + " }"
    }
  }

  case class Dependency(val tree: Tree, val symbol: Symbol, val dependency: Option[Dependency]) {
    override def toString = { symbol.name + (dependency match {
      case Some(dependency) => " -> " + dependency.toString
      case _ => ""
    }) }
  }

  case class Binding(val tree: Tree, tpe: Type) { override def toString = tree.symbol.name.toString }

  class RegistryWriter {
    def += (u: CompilationUnitInfo) = Writer ! Add(u)
    def toReader = new RegistryReader(Writer.entities, Writer.units)

    private case class Add(u: CompilationUnitInfo)

    private object Writer extends Actor {
      var entities = HashMap[Type, Entity]()
      var units = HashMap[SourceFile, CompilationUnitInfo]()

      def act() = loop(react {
        case Add(u) => {
          (u.contexts ++ u.components).foreach((e) => {
            entities += e.tree.tpe -> e
          })
          units += u.source -> u
        }
      })
    }

    Writer.start
  }

  class RegistryReader(val entities: Map[Type, Entity], val units: Map[SourceFile, CompilationUnitInfo]) {
    def apply(u: SourceFile): Option[CompilationUnitInfo] = units.get(u)

    def getContext(t: Type): Option[Context] = entities.get(t) match {
      case Some(e: Context) => Some(e)
      case _ => None
    }

    def getComponent(t: Type): Option[Component] = entities.get(t) match {
      case Some(e: Component) => Some(e)
      case _ => None
    }
  }

}

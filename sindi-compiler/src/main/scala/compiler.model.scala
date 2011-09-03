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

  case class CompilationUnitInfo(source: SourceFile, contexts: List[Context], components: List[Component])

  case class Context(tree: Tree, modules: List[Type], bindings: List[Binding], dependencies: List[Dependency]) extends Entity

  case class Component(tree: Tree, modules: List[Type], dependencies: List[Dependency]) extends Entity

  sealed trait Entity {
    def tree: Tree 
    def dependencies: List[Dependency]
    def modules: List[Type]
    override def toString = tree.symbol.name.toString + {
      if (!dependencies.isEmpty) " { dependencies: " + dependencies.mkString(", ") + " }" else ""
    } + {
      if (!modules.isEmpty) " [ modules: " + modules.mkString(", ") + " ]" else ""
    }
  }

  case class Dependency(val tree: Tree, val symbol: Symbol, val dependency: Option[Dependency], name: String) {
    override def equals(that: Any) = that match {
      case that: Dependency => symbol.equals(that.symbol) && dependency.equals(that.dependency)
      case _ => false
    }

    override def hashCode = symbol.hashCode + {
      dependency match {
        case Some(dependency) => dependency.hashCode
        case _ => 0
      }
    }

    override def toString = { name + (dependency match {
      case Some(dependency) => " -> " + dependency.toString
      case _ => ""
    }) }
  }

  case class Binding(val tree: Tree, symbol: Symbol) { override def toString = symbol.name.toString }

  class RegistryWriter {
    def += (u: CompilationUnitInfo) = Writer ! Add(u)
    def toReader = new RegistryReader(Writer.entities, Writer.units)

    private case class Add(u: CompilationUnitInfo)

    private object Writer extends Actor {
      var entities = HashMap[Symbol, Entity]()
      var units = HashMap[SourceFile, CompilationUnitInfo]()

      def act() = loop(react {
        case Add(u) => {
          (u.contexts ++ u.components).foreach((e) => {
            entities += e.tree.symbol -> e
          })
          units += u.source -> u
        }
      })
    }

    Writer.start
  }

  class RegistryReader(val entities: Map[Symbol, Entity], val units: Map[SourceFile, CompilationUnitInfo]) {
    def apply(u: SourceFile): Option[CompilationUnitInfo] = units.get(u)

    def getContext(s: Symbol): Option[Context] = get(s) match {
      case Some(e: Context) => Some(e)
      case _ => None
    }

    def getComponent(s: Symbol): Option[Component] = get(s) match {
      case Some(e: Component) => Some(e)
      case _ => None
    }

    private def get(s: Symbol): Option[Entity] = entities.get(s)
  }
}

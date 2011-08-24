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

trait Registry 

trait Model extends Component {
  import global._

  case class CompilationUnitInfo(unit: CompilationUnit, contexts: List[Context], components: List[Component])

  case class Context(tree: Tree, dependencies: List[Dependency], modules: List[Type]) extends Entity
  case class Component(tree: Tree, module: Type, dependencies: List[Dependency]) extends Entity

  case class Dependency(val tree: Tree, val entity: Entity, val dependency: Option[Dependency])
  case class Binding(val tree: Tree, tpe: Type)


  class RegistryWriter extends model.Registry {
    def += (u: CompilationUnitInfo) = Writer ! Add(u)
    def toReader = new RegistryReader(Writer.entities, Writer.units)

    private case class Add(u: CompilationUnitInfo)

    private object Writer extends Actor {
      var entities = HashMap[Type, Entity]()
      var units = HashMap[CompilationUnit, CompilationUnitInfo]()

      def act() = loop(react {
        case Add(u) => {
          (u.contexts ++ u.components).foreach((e) => {
            entities += e.tree.tpe -> e
          })
          units += u.unit -> u
        }
      })
    }

    Writer.start
  }

  class RegistryReader(val entities: Map[Type, Entity], val units: Map[CompilationUnit, CompilationUnitInfo])
      extends model.Registry {
    def apply(u: CompilationUnit): Option[CompilationUnitInfo] = units.get(u)

    def getContext(t: Type): Option[Context] = entities.get(t) match {
      case Some(e: Context) => Some(e)
      case _ => None
    }

    def getComponent(t: Type): Option[Component] = entities.get(t) match {
      case Some(e: Component) => Some(e)
      case _ => None
    }
  }

  private[model] trait Entity { def tree: Tree }
}

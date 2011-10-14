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
import scala.util.parsing.json._

import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin
import nsc.util.SourceFile

import utils.JSON._

// TODO [aloiscochard] Improve JSON support: Show bindings with full qualified names

abstract class ModelPlugin(val global: Global) extends Plugin {
  import global._

  var options: Options

  protected final val symContext = global.definitions.getClass(manifest[sindi.Context].erasure.getName)
  protected final val symComponent = global.definitions.getClass(manifest[sindi.Component].erasure.getName)
  protected final val symComponentContext = global.definitions.getClass(manifest[sindi.ComponentContext].erasure.getName)
  protected final val symComponentWith = global.definitions.getClass(manifest[sindi.ComponentWith[_]].erasure.getName)
  protected final val symComposable = global.definitions.getClass(manifest[sindi.Composable].erasure.getName)
  protected final val symInjector = global.definitions.getClass(manifest[sindi.injector.Injector].erasure.getName)
  protected final val symModule = global.definitions.getClass(manifest[sindi.Module].erasure.getName)
  protected final val symModuleT = global.definitions.getClass(manifest[sindi.ModuleT[_]].erasure.getName)
  protected final val symModuleManifest = global.definitions.getClass(manifest[sindi.ModuleManifest[_]].erasure.getName)

  case class CompilationUnitInfo(source: SourceFile, contexts: List[Context], components: List[Entity]) {
    override def toString = prettyFormatter(toJson)
    def toJson = JSONObject(Map(
      "source" -> source.toString,
      "contexts" -> JSONArray(contexts.map(_.toJson)),
      "components" -> JSONArray(components.map(_.toJson))
    ))
  }

  case class Context(tree: ClassDef, modules: List[Module],
                      bindings: List[Binding], dependencies: List[Dependency]) extends Entity

  case class Component(tree: ClassDef, modules: List[Module], dependencies: List[Dependency]) extends Entity {
    val bindings: List[Binding] = Nil
  }

  case class ComponentWithContext(tree: ClassDef, context: String, dependencies: List[Dependency]) extends Entity {
    val bindings: List[Binding] = Nil
    val modules: List[Module] = Nil
    override def toString = "[" + context + "] " + super.toString 
  }

  sealed trait Entity {
    def tree: ClassDef 
    def dependencies: List[Dependency]
    def modules: List[Module]
    def bindings: List[Binding]
    override def toString = prettyFormatter(toJson)
    def toJson = JSONObject(Map(
      // TODO [aloiscochard] Improve name retrieval for anonymous class and others jewels
      "name" -> { if (tree.symbol.tpe.toString.contains(" with ")) { tree.symbol.name.toString } else tree.symbol.tpe.toString },
      "dependencies" -> JSONArray(dependencies.map(_.toString).distinct),
      "modules" -> JSONArray(modules.map(_.toString).distinct),
      "bindings" -> JSONArray(bindings.map(_.toString).distinct)
    ))
  }

  case class Signature(symbol: Symbol, tpe: Option[Type] = None)

  case class Dependency(tree: Tree, signature: Signature, dependency: Option[Dependency], name: String) {
    def symbol = signature.symbol
    override def toString = { name + (dependency match {
      case Some(dependency) => " -> " + dependency.toString
      case _ => ""
    }) }
  }

  case class Binding(tree: Tree, symbol: Symbol) { override def toString = symbol.tpe.toString }
  case class Module(symbol: Symbol, tpe: Type, name: String, inferred: Option[Dependency] = None) {
    override def toString = name
  }

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

    def getContext(s: String): Option[Context] = entities.find((e) => {
      (e._1.fullName.compareTo(s) == 0)
    }) match {
      case Some(( _, e: Context)) => Some(e)
      case _ => None
    }

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

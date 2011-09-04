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
package reader 

import scala.annotation.tailrec
import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import model.ModelPlugin

// TODO [acochard] check if component import needed modulemanifest otherwise optionally generate them
// TODO [acochard] add support for injectAs
// TODO [acochard] add support for ModuleT
//
abstract class ReaderPlugin (override val global: Global) extends ModelPlugin(global) {
  import global._

  private final val symContext = global.definitions.getClass(manifest[sindi.Context].erasure.getName)
  private final val symComponent = global.definitions.getClass(manifest[sindi.Component].erasure.getName)
  private final val symComposable = global.definitions.getClass(manifest[sindi.Composable].erasure.getName)
  private final val symInjector = global.definitions.getClass(manifest[sindi.injector.Injector].erasure.getName)
  private final val symModule = global.definitions.getClass(manifest[sindi.Module].erasure.getName)
  private final val symModuleT = global.definitions.getClass(manifest[sindi.ModuleT[_]].erasure.getName)
  private final val symModuleManifest = global.definitions.getClass(manifest[sindi.ModuleManifest[_]].erasure.getName)

  def read(unit: CompilationUnit, body: Tree, registry: RegistryWriter): Unit = {
    var contexts: List[Context] = Nil
    var components: List[Component] = Nil

    // TODO [aloiscochard] Better synchronized granularity
    for (tree @ ClassDef(_, _, _, _) <- unit.body) {
      if (global.synchronized { isContext(tree) }) {
        contexts = global.synchronized { createContext(tree) } :: contexts
      } else if (global.synchronized { isComponent(tree) }) {
        components = global.synchronized { createComponent(tree) } :: components
      }  
    }
    
    if ((!contexts.isEmpty || !components.isEmpty)) {
      if (options.verbose) global.synchronized {
        global.inform(unit + " {\n" +
          { if (!contexts.isEmpty) contexts.map("\t" + _ + "\n").mkString else "" } +
          { if (!components.isEmpty) components.map("\t" + _ + "\n").mkString else "" } +
          "}")
      }
      registry += CompilationUnitInfo(unit.source, contexts, components)
    }
  }
  //global.treeBrowsers.create().browse(tree)

  private def createContext(tree: ClassDef): Context = {
    new Context(tree, getModules(tree), getBindings(tree), getDependencies(tree))
  }

  private def createComponent(tree: ClassDef): Component = {
    new Component(tree, getModulesFromComponent(tree), getDependencies(tree))
  }

  private def getModules(tree: ClassDef) = {
    collect[DefDef](tree.children)((tree) => tree match {
      case tree: DefDef => if (tree.name.toString == "modules") Some(tree) else None
      case _ => None
    }).headOption match {
      case Some(tree) => {
        collect[ValDef](tree.children)((tree) => tree match {
          case tree: ValDef => Some(tree)
          case _ => None
        }).map((tree) => {
          Module(tree.symbol.tpe.typeSymbol, tree.symbol.tpe.toString)
        })
      }
      case None => Nil
    }
  }

  private def getModulesFromComponent(root: ClassDef) = {
    var types = List[String]()
    collect[TypeTree](root.children)((tree) => tree match {
      case tree: TypeTree => {
        if (tree.symbol.exists &&
            isComponent(tree) && !isContext(tree) &&
            !(tree.symbol.classBound =:= symComponent.classBound) &&
            !(tree.symbol.classBound =:= root.symbol.classBound)) {
          Some(tree)
        } else None
      }
      case _ => None
    }).foreach((tree) => {
      tree.tpe.members.filter((s) => {
        s.isValue && (s.tpe.typeSymbol.isSubClass(symModuleManifest))
      }).foreach((s) => {
        types = getTypeParam(s.tpe.toString) :: types
      })
    })
    types.distinct.map((s) => { Module(global.definitions.getClass(s), s) })
  }

  protected def getBindings(tree: ClassDef): List[Binding] = {
    var bindings = List[Binding]()
    for (tree @ ValDef(_, _, _, _) <- tree.impl.body) {
      if (tree.name.toString.trim == "bindings") {
        collect[TypeTree](tree.children)((tree) => tree match {
          case tree: TypeApply => {
            def isBind(tree: Tree) = tree.children.headOption match {
              case Some(tree: Select) => tree.name.toString.trim == "bind"
              case _ => false
            }
            if (isBind(tree)) { 
              tree.children.collectFirst({ case t: TypeTree => t})
            } else {
              None
            }
          }
          case _ => None
        }).foreach((tree) => { bindings = Binding(tree,tree.symbol) :: bindings })
      }
    }
    bindings
  }

  private def getDependencies(root: Tree): List[Dependency] = {
    def getDependency(tree: Tree) = {
      val injected = Dependency(tree, tree.tpe.typeSymbol, None, tree.tpe.toString)
      def get(tree: Tree, dependency: Dependency): Dependency = {
        find[TypeApply](List(tree))((tree) => tree match {
          case tree: TypeApply => if (tree.symbol.name.toString == "from") Some(tree) else None
          case _ => None
        }) match {
          case Some(tree) => {
            tree.children.collectFirst({ case t: TypeTree => t}) match {
              case Some(typeTree) => {
                val d = if (dependency.symbol.toString == typeTree.symbol.toString) dependency else
                          Dependency(tree, typeTree.symbol, Some(dependency), typeTree.tpe.toString)
                get(tree.children.head, d)
              }
              case _ => dependency
            }
          }
          case _ => dependency
        }
      }
      get(tree, injected)
    }

    // Adding direct dependencies
    val dependencies = collect[Tree](root.children)((tree) => tree match {
      case tree: Apply => {
        if (tree.symbol.name.toString == "inject" && tree.symbol.owner.isSubClass(symInjector)) {
          Some(tree)
        } else { None }
      }
      case _ => None
    }).map(getDependency(_))

    // Adding imported dependencies (from[T].*)
    val imported = collect[Tree](root.children)((tree) => tree match {
      case tree: Apply => if (tree.symbol.owner.isSubClass(symComposable)) Some(tree) else None
      case _ => None
    }).map(getDependency(_)).filter((tree) => {
      (tree.symbol.name.toString != "<none>") &&
      (tree.symbol != symModule) &&
      (tree.symbol != symModuleT)
    })

    // Adding mixed-in component's dependencies
    var infered = List[Dependency]() // WARNING: Mutable, compiler crash if using 'val' and 'reduce'
    collect[TypeTree](root.children)((tree) => tree match {
      case tree: TypeTree => {
        if (tree.symbol.exists &&
            isComponent(tree) && !isContext(tree) &&
            !(tree.symbol.classBound =:= symComponent.classBound) &&
            !(tree.symbol.classBound =:= root.symbol.classBound)) {
          Some(tree)
        } else None
      }
      case _ => None
    }).foreach((tree) => {
      tree.tpe.members.filter((s) => {
        s.isValue && (s.tpe.typeSymbol.isSubClass(symModuleManifest))
      }).map((s) => {
        val typeParamName = getTypeParam(s.tpe.toString)
        infered = Dependency(tree, global.definitions.getClass(typeParamName), None, typeParamName) :: infered
      })
    })

    (dependencies ++ imported ++ infered).distinct
  }

  private def isContext(tree: Tree) = tree.symbol.classBound <:< symContext.classBound
  private def isComponent(tree: Tree) = tree.symbol.classBound <:< symComponent.classBound

  ///////////////////
  // AST Utilities //
  ///////////////////

  /** Find first matching tree using Depth First Search **/
  private def find[T <: AnyRef](lookup: List[Tree])(filter: (Tree) => Option[T]): Option[T] = {
    for(tree <- lookup) {
      filter(tree) match {
        case Some(tree) => return Some(tree)
        case _ =>
      }
      find[T](tree.children)(filter) match {
        case Some(tree) => return Some(tree)
        case _ =>
      }
    }
    return None
  }

  /** Collect all matchings trees **/
  @tailrec
  private def collect[T <: AnyRef](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { found.flatten ++ accumulator }
    else { collect(children, found.flatten ++ accumulator)(filter) }
  }
  
  // TODO [aloiscochard] Fix that ugly hack to take component type paramete
  private def getTypeParam(typeName: String) = {
    var paramName = typeName.slice(typeName.indexOf("[") + 1, typeName.lastIndexOf("]"))
    if (paramName.contains("[")) { paramName = paramName.slice(0, paramName.indexOf("[")) }
    paramName
  }
}

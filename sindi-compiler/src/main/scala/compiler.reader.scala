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

// TODO [acochard] add support for injectAs
// TODO [acochard] add support for ModuleT
//
abstract class ReaderPlugin (override val global: Global) extends ModelPlugin(global) {
  import global._

  private final val symContext = global.definitions.getClass(manifest[sindi.Context].erasure.getName)
  private final val symComponent = global.definitions.getClass(manifest[sindi.Component[_]].erasure.getName)
  private final val symInjector = global.definitions.getClass(manifest[sindi.injector.Injector].erasure.getName)

  def read(unit: CompilationUnit, body: Tree, registry: RegistryWriter): Unit = {
    var contexts: List[Context] = Nil
    var components: List[Component] = Nil

    for (tree @ ClassDef(_, _, _, _) <- unit.body) {
      if (isContext(tree)) {
        contexts = createContext(tree) :: contexts
      } else if (isComponent(tree)) {
        components = createComponent(tree) :: components
      }  
    }
    
    if ((!contexts.isEmpty || !components.isEmpty)) {
      if (options.verbose) {
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
    // TODO [aloiscochard] Fix that ugly hack to take component type parameter
    val module = find[Symbol](List(tree))((tree) => tree match {
      case tree: TypeTree => {
        global.synchronized {
          val typeName = tree.tpe.toString
          if (typeName.startsWith("sindi.Component") && typeName.contains("[")) {
            var moduleName = typeName.slice(typeName.indexOf("[") + 1, typeName.lastIndexOf("]"))
            if (moduleName.contains("[")) { moduleName = moduleName.slice(0, moduleName.indexOf("[")) }
            Some(global.definitions.getClass(moduleName))
          } else None
        }
      }
      case _ => None
    })
    new Component(tree, module, getDependencies(tree))
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
        }) map (_.symbol.tpe)
      }
      case None => Nil
    }
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
        }).foreach((tree) => { bindings = Binding(tree,tree.tpe) :: bindings })
      }
    }
    bindings
  }

  private def getDependencies(root: Tree): List[Dependency] = {
    def getDependency(tree: Tree) = {
      val injected = Dependency(tree, tree.tpe.typeSymbol, None)
      def get(tree: Tree, dependency: Dependency): Dependency = {
        find[TypeApply](List(tree))((tree) => tree match {
          case tree: TypeApply => if (tree.symbol.name.toString == "from") Some(tree) else None
          case _ => None
        }) match {
          case Some(tree) => {
            tree.children.collectFirst({ case t: TypeTree => t}) match {
              case Some(typeTree) => {
                val d = if (dependency.symbol.toString == typeTree.symbol.toString) dependency else 
                          Dependency(tree, typeTree.symbol, Some(dependency))
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

    val dependencies = collect[Tree](root.children)((tree) => tree match {
      case tree: Apply => {
        if ((tree.symbol.name.toString == "inject" || tree.symbol.name.toString == "injectAs") && 
            tree.symbol.owner.isSubClass(symInjector)) Some(tree) else None
      }
      case _ => None
    }).map(getDependency(_)) ++
    collect[Tree](root.children)((tree) => tree match {
      case tree: Apply => {
        if (tree.symbol.owner.isSubClass(symInjector)) Some(tree) else None
      }
      case _ => None
    }).map(getDependency(_)).filter(_.symbol.name.toString != "<none>")

    // Adding mixed-in component's dependencies
    val infered = global.synchronized {
      collect[Dependency](root.children)((tree) => tree match {
        case tree: TypeTree => {
          if (tree.symbol.exists &&
              isComponent(tree) && !isContext(tree) &&
              !(tree.symbol.classBound =:= symComponent.classBound) &&
              !(tree.symbol.classBound =:= root.symbol.classBound)) {
            Some(Dependency(tree, tree.symbol, None)) 
          } else None
        }
        case _ => None
      })
    }
    dependencies ++ infered
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
  private final def collect[T <: AnyRef](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { found.flatten ++ accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
  }
}

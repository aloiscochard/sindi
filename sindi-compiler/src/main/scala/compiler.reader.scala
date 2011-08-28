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

// TODO [acochard] add support for binding not created with DSL ? I think no
// TODO [acochard] add support for injectAs
abstract class ReaderPlugin (override val global: Global) extends ModelPlugin(global) {
  import global._

  private final val symContext = global.definitions.getClass(manifest[sindi.Context].erasure.getName)
  private final val symComponent = global.definitions.getClass(manifest[sindi.Component[_]].erasure.getName)
  private final val symInjector = global.definitions.getClass(manifest[sindi.injector.Injector].erasure.getName)

  def read(unit: CompilationUnit, body: Tree, registry: RegistryWriter): Unit = {
    var contexts: List[Context] = Nil
    var components: List[Component] = Nil

    //println("read.a: " + unit)

    for (tree @ ClassDef(_, _, _, _) <- unit.body) {
      /*
      def browse(tree: Tree) {
        tree.children.foreach(browse(_))
        tree.tpe
        tree.symbol
        //println(tree.symbol.owner)
        //tree.symbol.tpe
      }
      browse(tree)
      */
      if (isContext(tree)) {
        //println("component: " + tree.name)
        contexts = createContext(tree) :: contexts
        // TODO Check if context mixin component and add corresponding dependencies
      } else if (isComponent(tree)) {
        //println("context: " + tree.name)
        components = createComponent(tree) :: components
      }  
    }

    //println("read.b: " + unit)
    
    println("""
    ------------------------------------
    """ + unit + """
    """ + contexts + """
    """ + components + """
    ------------------------------------
    """)

    registry += CompilationUnitInfo(unit.source, contexts, components)
  }
  //global.treeBrowsers.create().browse(tree)


  private def createContext(tree: ClassDef): Context = {
    new Context(tree, getModules(tree), getBindings(tree), getDependencies(tree))
  }

  private def createComponent(tree: ClassDef): Component = {
    // TODO [aloiscochard] Fix that ugly hack to take component type parameter
    val module = find[Symbol](List(tree))((tree) => tree match {
      case tree: TypeTree => {
        val typeName = tree.tpe.toString
        if (typeName.startsWith("sindi.Component") && typeName.contains("[")) {
          var moduleName = typeName.slice(typeName.indexOf("[") + 1, typeName.lastIndexOf("]"))
          if (moduleName.contains("[")) { moduleName = moduleName.slice(0, moduleName.indexOf("[")) }
          Some(global.definitions.getClass(moduleName))
        } else None
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

  private def getDependencies(tree: Tree): List[Dependency] = {
    collect[Tree](tree.children)((tree) => tree match {
      case tree: Apply => {
        if (tree.symbol.name.toString == "inject" && tree.symbol.owner.isSubClass(symInjector)) {
          Some(tree)
        } else { None }
      }
      case _ => None
    }).map((tree) => {
      val injected = Dependency(tree, tree.tpe, None)
      def getDependency(tree: Tree, dependency: Dependency): Dependency = {
        find[TypeApply](List(tree))((tree) => tree match {
          case tree: TypeApply => if (tree.symbol.name.toString == "from") { Some(tree) } else None
          case _ => None
        }) match {
          case Some(tree) => {
            tree.children.collectFirst({ case t: TypeTree => t}) match {
              case Some(typeTree) => {
                getDependency(tree.children.head, Dependency(tree, typeTree.tpe, Some(dependency)))
              }
              case _ => dependency
            }
          }
          case _ => dependency
        }
      }
      getDependency(tree, injected)
    })
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
  private final def collect[T <: Tree](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { found.flatten ++ accumulator } else { collect(children, found.flatten ++ accumulator)(filter) }
  }
}

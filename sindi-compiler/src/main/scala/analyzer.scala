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
package analyzer 

import scala.annotation.tailrec
import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import model.ModelPlugin

// TODO [acochard] add injectAs support
// TODO [acochard] check ModuleT support
// TODO [acochard] actually inline component have '$anon' as name, find workaround if possible

abstract class AnalyzerPlugin(override val global: Global) extends ComponentPlugin(global) {
  import global._

  def read(unit: CompilationUnit, registry: RegistryWriter): Unit = {
    val _contexts = contexts(unit)
    var _components = components(unit)
    
    if ((!_contexts.isEmpty || !_components.isEmpty)) {
      val info = CompilationUnitInfo(unit.source, _contexts, _components)
      if (options.verbose) global.synchronized {
        global.inform(info.toString)
      }
      registry += info
    }
  }
  //global.treeBrowsers.create().browse(tree)


  // TODO [aloiscochard] Better synchronized granularity
  protected def contexts(unit: CompilationUnit) = {
    (for (tree @ ClassDef(_, _, _, _) <- unit.body) yield {
      global.synchronized {
        if (isContext(tree)) Some(createContext(tree))
        else None
      }
    }).flatten
  }

  protected def components(unit: CompilationUnit) = {
    (for (tree @ ClassDef(_, _, _, _) <- unit.body) yield {
      global.synchronized {
        if (!isContext(tree) && isComponent(tree)) Some(createComponent(tree))
        else None
      }
    }).flatten
  }

  private def isContext(tree: Tree) = tree.symbol.classBound <:< symContext.classBound

  private def isComponent(tree: Tree) = tree.symbol.classBound <:< symComponent.classBound
}

abstract class AnalyzisPlugin(override val global: Global) extends ModelPlugin(global) {
  import global._

  protected def getTypeDependencies(tpe: Type): List[String] = {
    tpe.baseClasses.flatMap((s) => {
      s.classBound.members.filter((s) => s.isValue && (s.tpe.typeSymbol.isSubClass(symModuleManifest)))
        .map((s) => getTypeParam(s.tpe.toString))
    }).distinct
  }

  // TODO [aloiscochard] Fix that ugly hack to take component type parameter
  protected def getTypeParam(typeName: String) = {
    var paramName = typeName.slice(typeName.indexOf("[") + 1, typeName.lastIndexOf("]"))
    if (paramName.contains("[")) { paramName = paramName.slice(0, paramName.indexOf("[")) }
    if (paramName.endsWith(".type")) { paramName = paramName.substring(0, paramName.size - 5) }
    paramName
  }

  ///////////////////
  // AST Utilities //
  ///////////////////

  /** Find first matching tree using Depth First Search **/
  protected def find[T <: AnyRef](lookup: List[Tree])(filter: (Tree) => Option[T]): Option[T] = {
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
  protected final def collect[T <: AnyRef](lookup: List[Tree], accumulator: List[T] = Nil)
      (filter: (Tree) => Option[T]): List[T] = {
    var children = List[Tree]()
    val found = for(tree <- lookup) yield { children = children ++ tree.children; filter(tree) }
    if (children.isEmpty) { found.flatten ++ accumulator }
    else { collect(children, found.flatten ++ accumulator)(filter) }
  }
}


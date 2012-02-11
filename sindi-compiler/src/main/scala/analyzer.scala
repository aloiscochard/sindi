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

// TODO [acochard] add injectAs support
// TODO [acochard] check ModuleT support
// TODO [acochard] actually inline component have '$anon' as name, find workaround if possible

trait Analyzer extends SindiPlugin with ContextAnalyzis with ComponentAnalyzis {
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

trait Analyzis extends SindiPlugin {
  import global._

  protected def getDependencies(root: Tree): List[Dependency] = {
    def getDependency(tree: Tree) = {
      val injected = Dependency(tree, Signature(tree.tpe.typeSymbol, Some(tree.tpe)), None, tree.tpe.toString)
      def get(tree: Tree, dependency: Dependency): Dependency = {
        find[TypeApply](List(tree))((tree) => tree match {
          case tree: TypeApply => if (tree.symbol.name.toString == "from") Some(tree) else None
          case _ => None
        }) match {
          case Some(tree) => {
            tree.children.collectFirst({ case t: TypeTree => t}) match {
              case Some(typeTree) => {
                val d = if (dependency.symbol.toString == typeTree.symbol.toString) dependency else
                          Dependency(tree, Signature(typeTree.symbol), Some(dependency), typeTree.tpe.toString)
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

    def isInjectorMethod(t: Tree, n: String) = t.symbol.name.toString == n && t.symbol.owner.isSubClass(symInjector)
    def isInject(tree: Apply) = isInjectorMethod(tree, "inject")
    def isInjectAs(tree: Apply) = isInjectorMethod(tree,"injectAs") && tree.tpe.boundSyms.isEmpty

    def isNone(tree: Tree) = tree match {
      case tree: Select if tree.toString == "scala.None" => true
      case _ => false
    }

    def getQualifiers(tree: Tree) = {
      // TODO Add support of dynamic qualifier (injectAs[Foo](qualifier[Bar] || "foo"))
      val qualifiers = find[Apply](tree.children)((t) => t match {
        case t: Apply if t.tpe.typeSymbol.isSubClass(symQualifiers) => Some(t)
        case _ => None
      }).map((tree) => collect[TypeTree](tree.children)((tree) => tree match {
        case tree: TypeTree => Some(tree)
        case _ => None
      })).map(_.map(_.tpe).distinct).getOrElse(Nil)

      if (traverse(tree).exists(isNone _)) qualifiers :+ symNone.tpe else qualifiers
    }

    // Adding direct dependencies
    val dependencies = collect[Dependency](root.children)((tree) => tree match {
      case tree: Apply if isInject(tree) => Some(getDependency(tree))
      case tree: Apply if isInjectAs(tree) =>
        Some(Dependency(tree, Signature(tree.tpe.typeSymbol, Some(tree.tpe)), None, tree.tpe.toString, getQualifiers(tree)))
      case _ => None
    })

    // Adding imported dependencies (from[T].*)
    val (imported, components) = collect[Tree](root.children)((tree) => tree match {
      case tree: Apply =>
        if (tree.symbol.owner.isSubClass(symComposable) && 
            // Filtering inline module definition
            !(tree.symbol.owner.isSubClass(symContext) && tree.symbol.name.toString == "module")) 
          Some(tree)
        else None
      case _ => None
    }).map(getDependency(_)).filter((tree) => {
      (tree.symbol.name.toString != "<none>") &&
      (tree.symbol != symModule) &&
      (tree.symbol != symModuleManifest) &&
      (tree.symbol != symModuleT) &&
      (tree.symbol != symComponentContext)
    }).partition((d) =>
      // Filtering ComponentContext instantiation
      !(d.symbol.classBound <:< symComponentContext.classBound && !(d.symbol.classBound <:< symModule.classBound))
    )

    var inferred = List[Dependency]() // WARNING: Mutable, compiler crash if using 'val' and 'reduce'

    // Adding mixed-in (with Component) dependencies
    getTypeDependencies(root.symbol.classBound).foreach((s) => 
      inferred = Dependency(root, Signature(global.definitions.getClass(s)), None, s) :: inferred)

    // Resolve ComponentContext intantiation into component dependencies
    components.foreach((dependency) => {
      if (dependency.symbol.isSubClass(symComponentWith)) {
        // Ignoring ComponentWith (they are statically checked using their linked context)
      } else {
        getTypeDependencies(dependency.tree.tpe).foreach((s) => 
            inferred = new Dependency(dependency.tree, Signature(global.definitions.getClass(s)), None, s) :: inferred)
      }
    })

    (dependencies ++ imported ++ inferred)
  }

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
}


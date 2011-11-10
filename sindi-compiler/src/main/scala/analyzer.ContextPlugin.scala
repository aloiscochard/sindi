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

import scala.tools.nsc
import nsc.Global 

abstract class ContextPlugin (override val global: Global) extends AnalyzisPlugin(global) {
  import global._

  protected final val symNone = global.definitions.getClass(manifest[scala.None.type].erasure.getName)

  protected def createContext(tree: ClassDef) =
    new Context(tree, getModules(tree), getBindings(tree), getDependencies(tree))

  protected def getModules(tree: ClassDef) = {
    collect[DefDef](tree.children)((tree) => tree match {
      case tree: DefDef => if (tree.name.toString == "modules") Some(tree) else None
      case _ => None
    }).headOption match {
      case Some(tree) => {
        collect[ValDef](tree.children)((tree) => tree match {
          case tree: ValDef => Some(tree)
          case _ => None
        }).map((tree) => {
          Module(tree.symbol, tree.symbol.tpe, tree.symbol.tpe.toString)
        })
      }
      case None => Nil
    }
  }

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

    // Adding direct dependencies
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

  private def getBindings(tree: ClassDef): List[Binding] = {
    def typeOf(tree: Tree) = tree.children.collectFirst { case t: TypeTree => t }
    def isSelectOfBind(tree: Tree) = isSelectOf(tree, "bind")
    def isSelectOfAs(tree: Tree) = isSelectOf(tree, "as")
    def isSelectOf(tree: Tree, name: String) = (tree, tree.children.headOption) match {
      case (_: TypeApply, Some(tree: Select)) => tree.name.toString.trim == name
      case _ => false
    }

    val backtrack = (tree: Tree) => tree match {
      case _ if isSelectOfAs(tree) => true
      case _ if isSelectOfBind(tree) => true
      case _ => false
    }
    // TODO does it work ok with val? (/!\ compiler crash)
    var bindings = List[Binding]()
    for (tree @ ValDef(_, _, _, _) <- tree.impl.body if tree.name.toString.trim == "bindings") {
      traversal(tree.children)(backtrack).flatMap(_ match {
        case t if isSelectOfAs(t) => Some(traverse(t).find(isSelectOfBind(_)).flatMap(typeOf(_)) -> typeOf(t).map(_.tpe))
        case t if isSelectOfBind(t) => Some(typeOf(t) -> None)
        case _ =>
          None
      }).flatMap(_ match {
        case (Some(tree), qualifier) => Some(Binding(tree, tree.symbol, qualifier))
        case _ => None
      }).foreach(b => { bindings = b :: bindings })
    }
    bindings
  }
}


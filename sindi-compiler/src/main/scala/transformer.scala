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
package transformer 

import scala.tools.nsc
import nsc.Global 
import nsc.ast.TreeDSL
import nsc.transform.TypingTransformers 
import nsc.symtab.Flags._

import analyzer.AnalyzerPlugin

abstract class TransformerPlugin(override val global: Global) extends AnalyzerPlugin(global)
                                                                with TypingTransformers 
                                                                with TreeDSL {
  import global._

  def transform(unit: CompilationUnit): Unit = {
    // TODO Get only inferred dependencies
    val _components = components(unit)
    if (!_components.isEmpty) new ComponentTransformer(unit, _components).transformUnit(unit)
  }

  private class ComponentTransformer(unit: CompilationUnit, components: List[Entity])
      extends TypingTransformer(unit) {

    import CODE._
    override def transform(tree: global.Tree) : global.Tree = {
      val newTree = tree match {
        case cd @ ClassDef(mods, name, tparams, impl) => {
          components.find(_.tree.symbol == tree.symbol) match {
            case Some(component) => {
              // Automatically generate ModuleManifest for inferred dependencies
              val clazz = cd.symbol
              val trees = component.modules.flatMap(_.inferred).flatMap((dependency) => {
                val name = dependency.name.toString.replaceAllLiterally(".", "$")
                val tpe = appliedType(symModuleManifest.tpe, List(dependency.tree.tpe))
                List[Tree]({
                  // VAL
                  val valSym = clazz.newValue(clazz.pos.focus, newTermName(name + " "))
                  valSym.setFlag(PRIVATE | LOCAL)
                  valSym.setInfo(tpe)
                  clazz.info.decls enter valSym
                  localTyper.typed(VAL(valSym) === NEW(TypeTree(tpe)))
                },{
                  // DEF
                  val defSym = clazz.newMethod(clazz.pos.focus, newTermName(name))
                  defSym setFlag (METHOD | STABLE | ACCESSOR)
                  defSym setInfo MethodType(defSym.newSyntheticValueParams(Nil), tpe)
                  clazz.info.decls enter defSym
                  localTyper.typed(DEF(defSym) === { THIS(clazz) DOT newTermName(name + " ") })
                })
              })
              treeCopy.ClassDef(tree, mods, name, tparams,
                treeCopy.Template(impl, impl.parents, impl.self, trees ::: impl.body))
            }
            case _ => tree
          }
        }
        case _ => tree
      }
      super.transform(newTree)
    }
  }

}


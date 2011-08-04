//      _____         ___  
//     / __(_)__  ___/ (_)
//    _\ \/ / _ \/ _  / /
//   /___/_/_//_/\_,_/_/
//
//  (c) 2011, Alois Cochard
//
//  http://aloiscochard.github.com/sindi
//

package sindi 
package compiler 

import scala.annotation.tailrec
import scala.tools.nsc 

import nsc.Global 
import nsc.Phase 
import nsc.ast.TreeBrowsers
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

class DependencyChecker(val global: Global) extends Plugin {
  import global._

  val name = "sindi.depchecks"
  val description = "Checks if Sindi's modules dependencies are satisfated"
  val components = List[PluginComponent](Component)

  private object Component extends PluginComponent {
    val global: DependencyChecker.this.global.type = DependencyChecker.this.global
    val runsAfter = List[String]("refchecks")
    val phaseName = DependencyChecker.this.name

    def newPhase(_prev: Phase) = new DependencyCheckerPhase(_prev)

    class DependencyCheckerPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DependencyChecker.this.name

      def apply(unit: CompilationUnit) {

        // TODO [aloiscochard] @tailrec
        def implement[T : Manifest](tree: Tree): Boolean = {
          if (tree.tpe.toString == manifest[T].erasure.getName) return true
          tree match {
            case tree: ClassDef => {
              tree.impl.parents.foreach((p) => {
                if (implement[T](p)) return true
              })
              return false
            }
            case _ => false
          }
        }

        @tailrec def test(a: Int): Int = {
          if (a == 1) return 0
          return test(a + 1)
        }

        @tailrec def lookup(matcher: (Tree) => Boolean, trees: Tree*): Option[Tree] = {
          var children = List[Tree]()
          trees.flatMap((tree: Tree) => {
            if (matcher(tree)) {
              Some(tree)
            } else {
              children = children ++ tree.children
              None
            }
          }).headOption match {
            case Some(tree) => return Some(tree)
            case _ =>
          }
          if (children.isEmpty) return None
          return lookup(matcher, children:_*)
        }

        /*
        def lookup(tree: Tree, matcher: (Tree) => Boolean): Option[Tree] = {
          if (matcher(tree)) {
            return Some(tree)
          } else {
            tree.children.foreach((tree: Tree) =>
              lookup(tree, matcher) match {
                case Some(tree) => return Some(tree)
                case _ =>
              }
            )
            return None
          }
        }
        */

        def isComponent(tree: Tree) = implement[sindi.Component](tree)
        def isModule(tree: Tree) = implement[sindi.Module](tree)

        for (tree @ ClassDef(_, _, _, _) <- unit.body) {
          //println(tree.name)
          //println(tree.impl.parents)
          if (isModule(tree)) {
            println("[module]" + tree.name)
            for (tree @ ValDef(_, _, _,  _) <- tree.impl.body) {
              if (tree.tpt.tpe.toString == "List[sindi.binder.binding.Binding[_]]" ||
                  tree.tpt.tpe.toString == "sindi.package.Bindings") {
                //println(tree.rhs)
              }
            }
          }
          if (isComponent(tree)) {
            println("[component]" + tree.name)


            for (tree @ DefDef(_, _, _, _, _, _) <- tree.impl.body) {
              println(tree.rhs)
              //DependencyChecker.this.global.treeBrowsers.create().browse(tree) 
            }
          }
        }
        // allows to browse the whole AST
        //DependencyChecker.this.global.treeBrowsers.create().browse(unit.body) 
      }
    }
  }
}

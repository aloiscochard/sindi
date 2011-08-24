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

import model.Model

// TODO [aloiscochard] Think about par colletion usage!
trait Reader extends Component with Model {
  import global._

  def read(unit: CompilationUnit, registry: RegistryWriter): Unit = {
    var contexts: List[Context] = Nil
    var components: List[Component] = Nil

    for (tree @ ClassDef(_, _, _, _) <- unit.body) {
      /*
      if (isContext(tree)) {
        contexts = Context(tree) :: contexts
      } else if (isComponent(tree)) {
        components = Component(tree) :: components
      }
      */
    }

    registry += CompilationUnitInfo(unit, contexts, components)
  }
}

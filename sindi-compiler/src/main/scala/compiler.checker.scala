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
package checker 

import model.Registry

trait Checker extends Component {
  import global._

  def check(unit: CompilationUnit, registry: Registry) = None
}


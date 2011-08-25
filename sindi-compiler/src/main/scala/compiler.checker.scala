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

import scala.tools.nsc
import nsc.Global 
import nsc.plugins.Plugin

import reader.ReaderPlugin

abstract class CheckerPlugin(override val global: Global) extends ReaderPlugin(global) {
  import global._

  def check(unit: CompilationUnit, registry: RegistryReader) = None
}


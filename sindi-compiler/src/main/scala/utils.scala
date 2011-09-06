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
package utils 

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

import scala.actors.scheduler.ExecutorScheduler
import scala.tools.nsc
import nsc.Phase
import nsc.plugins.PluginComponent

abstract class ParallelPluginComponent extends PluginComponent {
  import global._

  abstract class ParallelPhase(prev: Phase) extends StdPhase(prev) {
    protected val timeout = 10 * 1000
    protected val executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime.availableProcessors)
    protected val scheduler = ExecutorScheduler(executor)
    
    override def run() = {
      super.run()
      scheduler.shutdown
      executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)
    }

    def async(unit: CompilationUnit, body: Tree): Unit

    final def apply(unit: CompilationUnit) = {
      val body = unit.body
      scheduler.execute { async(unit, body) }
    }
  }
}

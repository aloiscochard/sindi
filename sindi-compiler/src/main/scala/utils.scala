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

object JSON {
  import scala.util.parsing.json._

  val prettyFormatter: JSONFormat.ValueFormatter = {
    def create(level: Int): JSONFormat.ValueFormatter = (x: Any) => {
      x match {
        case s: String => "\"" + JSONFormat.quoteString(s) + "\""
        case jo: JSONObject =>
          "{\n" + 
          "\t" * (level + 1) + 
          jo.obj.map({
            case (k, v) => JSONFormat.defaultFormatter(k.toString) + ": " + create(level + 1)(v) 
          }).mkString(",\n" +
          "\t" * (level + 1)) +
          "\n" + 
          "\t" * level + 
          "}" 
        case ja: JSONArray => ja.toString(create(level))
        case other => other.toString
      }
    }
    create(0)
  }

}

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

    def async(unit: CompilationUnit): Unit

    final def apply(unit: CompilationUnit) = scheduler.execute { async(unit) }
  }
}

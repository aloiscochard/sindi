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
package binder        

object DSLSpec {
  class Test extends DSL {
    val bindings = List[binding.Binding[_]](
        //bind[String],                                                 -- Doesn't compile
        bind[String] to "sindi",
        //bind[String] to "sindi" to "sindi",                           -- Doesn't compile
        //bind[String] as "qualifier",                                  -- Doesn't compile
        bind[String] to "sindi" as "qualifier",
        //bind[String] to "sindi" as "qualifier" as "qualifier",        -- Doesn't compile
        //bind[String] to "sindi" as "qualifier" to "sindi",            -- Doesn't compile
        //bind[String] scope { "scope" },                               -- Doesn't compile
        bind[String] to "sindi" scope { "scope" },
        //bind[String] to "sindi" scope { "scope" } as "qualifier"      -- Doesn't compile
        //bind[String] to "sindi" scope { "scope" } to "sindi",         -- Doesn't compile
        //bind[String] to "sindi" scope { "scope" } scope { "scope" },  -- Doesn't compile
        bind[String] to "sindi" as "qualifier" scope { "scope" }
      )
  }
}

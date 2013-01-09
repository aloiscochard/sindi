//      _____         ___                       
//     / __(_)__  ___/ (_)                      
//    _\ \/ / _ \/ _  / /                       
//   /___/_/_//_/\_,_/_/                        
//                                              
//  (c) 2012, Alois Cochard                     
//                                              
//  http://aloiscochard.github.com/sindi        
//                                              

package sindi.test

trait FooPlugin { def name: String }
class DefaultFooPlugin extends FooPlugin { val name = "default" }
class ProvidedFooPlugin extends FooPlugin { val name = "provided" }

import sindi._

class AppContext extends Context {
  override val bindings = Bindings(
    bind[Int] to 42 as qualifier[Int],
    bind[String] to "sindi"
  )
  inject[Int]
  injectAs[Int](qualifier[String])
  injectAs[Int](qualifier[String] || qualifier[Int])
  injectAs[Int](qualifier[Int] || qualifier[String])
  inject[String]
  injectAs[String](qualifier[String]) 
  injectAs[String](qualifier[String] || None) 
}

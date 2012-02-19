import sindi._

class AppContext extends Context {
  override val bindings = Bindings(
    bind[Int] to 42,
    bind[String] to "sindi",
    bind[Long] to 1L as qualifier[Long]
  )

  def f1(x: Int, y: String) = x + y
  def f2(x: Int, y: Long) = x + y

  autowire(f1 _)
  autowire(f2 _)
  autowire((_: Int, _: String))
  autowire((_: Int, _: Long))
}

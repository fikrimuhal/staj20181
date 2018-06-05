import scala.scalajs.js

object Main extends js.JSApp {
  def main(): Unit = {
    println("Hello from js!")
    val lib = new MyLibrary
    println(lib.sq(2))
  }
}

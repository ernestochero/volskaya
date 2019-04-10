package model
import org.joda.time.DateTime

object test extends App {
  override def main(args: Array[String]): Unit = {
    println(s"time is ${DateTime.now()}")
  }
}

package model
import org.joda.time._
import org.joda.time.format.DateTimeFormat

object test extends App {
  override def main(args: Array[String]) = {
    val pattern = "dd-MM-yyyy hh:mm:ss aa"
    val beforeSleep = DateTime.now()

    val afterSleep = DateTime.now()
    println(s"before : ${beforeSleep}")
    println(s"after : ${afterSleep.getMillis}")
    val inputTime = afterSleep.toString(pattern)
    println(s"convert : ${inputTime}")
    val dt = DateTime.parse(inputTime,DateTimeFormat.forPattern(pattern))
    println(s"la xx ${dt.getMillis}")

    val orderTypes:List[_ <: OrderType] = List(DELIVERY, OPERATION)
    val findOrderType = orderTypes.find( c => c.toString == "DELIVERY")
    println(s"findOrderType ${ findOrderType }")

    val statusOrderType:List[_ <: StatusOrderType] = StatusOrderTypeList.getStatusOrderTypeList
  }
}

package models

import akka.http.scaladsl.model.DateTime
import org.bson.types.ObjectId

case class Order(orderTypeName: Option[String],
                 statusOrderTypeName: Option[String],
                 kilometers:Option[Double],
                 finalPrice: Option[Double],
                 isPaid: Option[Boolean],
                 paymentDateTime: Option[DateTime],
                 paymentMethod: Option[String],
                 goals: Option[List[Goal]] = None
                )

sealed trait CoordinateT {
  def latitude:Double
  def longitude: Double
  def getCoordinate:(Double, Double)
}

case class Coordinate(latitude:Double, longitude:Double) extends CoordinateT {
  override def getCoordinate: (Double, Double) = (latitude, longitude)
}

sealed trait OrderType {
  val description: String
}

case object DELIVERY extends OrderType {
  override val description: String = "week, months of the year"
}
case object OPERATION extends OrderType {
  override val description: String = "but depending on your needs it can also be a good approach:"
}

case object OrderTypeList {
  def getOrderTypeList: List[_ <: OrderType] = List(DELIVERY, OPERATION)
}

sealed trait StatusOrderType {
  val description: String
}

case object CLIENT_GENERATED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object ADMINISTRATOR_DECLINED extends StatusOrderType {
  override val description: String =  "week, months of the year"
}

case object ADMINISTRATOR_OFFERED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object CLIENT_ACCEPTED_OFFER extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object CYCLIST_ACCEPTED_OFFER extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object CLIENT_CANCELED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object CYCLIST_CANCELED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object ADMINISTRATOR_CANCELED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object FINISHED extends StatusOrderType {
  override val description: String = "week, months of the year"
}

case object StatusOrderTypeList {
  def getStatusOrderTypeList: List[_ <: StatusOrderType] = List(
    CLIENT_GENERATED, ADMINISTRATOR_DECLINED, ADMINISTRATOR_OFFERED,
    CLIENT_ACCEPTED_OFFER, CYCLIST_ACCEPTED_OFFER, CLIENT_CANCELED,
    ADMINISTRATOR_CANCELED, FINISHED
  )
}

// val orderTypes:List[_ <: OrderType] = DELIVERY :: OPERATION :: Nil

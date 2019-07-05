package models

import akka.http.scaladsl.model.DateTime
import org.bson.types.ObjectId
import org.joda.time.DateTimeZone
//TODO: change to DateTime on time in the future
// agregar direccion de entrega y recojo
// Asignado al ciclista ~ en camino al punto de recojo ~  se recogio el pedido  ~  en camino al punto de entrega  ~ se entrego correctamente
// guardar el ultimo estado
// lista de productos ....
// sacar la orden a otra collection , ahi relacionamos el userClient con el userCyclist
// add booleano cobrar ...
// direccion de recogo , direccion entrega
// fecha de pedido ...

// This version just has one route
case class Order2(_id: ObjectId,
                  clientId: Option[ObjectId],
                  cyclistId: Option[ObjectId],
                  route: Option[Route],
                  orderStates: List[OrderState] = List(),
                  payMethod: PayMethod,
                  created: DateTime = DateTime.now(DateTimeZone.UTC),
                  lastState: Option[OrderState]
                )

case class Order(orderTypeName: Option[String],
                 statusOrderTypeName: Option[String],
                 kilometers:Option[Double],
                 finalPrice: Option[Double],
                 isPaid: Option[Boolean],
                 paymentDateTime: Option[String],
                 paymentMethod: Option[String],
                 goals: Option[List[Goal]] = None)


case class Product(name:String, description: String, photo: Option[String])


sealed trait OrderStateT {
  val orderStateName: String
}

// update this with the respective orderStateName
case object A extends OrderStateT {
  override val orderStateName: String = "A"
}

case object B extends OrderStateT {
  override val orderStateName: String = "B"
}

case object B extends OrderStateT {
  override val orderStateName: String = "C"
}

case object B extends OrderStateT {
  override val orderStateName: String = "D"
}

case class OrderState(nameState: OrderStateT, startTime: Option[DateTime], endTime: Option[DateTime], description: Option[String], isFinished: Boolean)


sealed trait PayMethod {
  val payMethodName: String
}

case object Cash extends PayMethod {
  override val payMethodName: String = "cash"
}

case object Card extends PayMethod {
  override val payMethodName: String = "card"
}



sealed trait CoordinateT {
  def latitude:Double
  def longitude: Double
  def getCoordinate:(Double, Double)
  override def toString: String = s"$latitude, $longitude"
}

case class Coordinate(latitude:Double, longitude:Double) extends CoordinateT {
  override def getCoordinate: (Double, Double) = (latitude, longitude)
}

case class Route(startCoordinate: Coordinate, endCoordinate: Coordinate)


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

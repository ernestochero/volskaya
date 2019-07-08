package models

import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
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
case class Order(_id: ObjectId,
                  clientId: String,
                  cyclistId: String,
                  route: Option[Route],
                  price: Option[Double],
                  orderStates: List[OrderState] = List(),
                  payMethod: Option[PayMethod],
                  lastState: Option[OrderState],
                  created:DateTime = DateTime.now(DateTimeZone.UTC),
                )

case class Product(name:String, description: String, photo: Option[String])

sealed trait OrderStateT {
  val orderStateName: String
}

object OrderStateT {
  case object Assigned extends OrderStateT {
    override val orderStateName: String = "assigned"
  }

  case object WayToPickUpOrder extends OrderStateT {
    override val orderStateName: String = "wayToPickUpOrder "
  }

  case object PickedUpOrder extends OrderStateT {
    override val orderStateName: String = "pickedUpOrder"
  }

  case object WayToDeliverOrder extends OrderStateT {
    override val orderStateName: String = "wayToDeliverOrder"
  }

  case object DeliveredOrder extends OrderStateT {
    override val orderStateName: String = "deliveredOrder"
  }
}

sealed trait PayMethod {
  val payMethodName: String
}

object PayMethod {
  case object Cash extends PayMethod {
    override val payMethodName: String = "cash"
  }

  case object Card extends PayMethod {
    override val payMethodName: String = "card"
  }
}

sealed trait CoordinateT {
  def latitude:Double
  def longitude: Double
  def getCoordinate:(Double, Double)
  override def toString: String = s"$latitude, $longitude"
}

case class OrderState(nameState: OrderStateT, startTime: Option[DateTime], endTime: Option[DateTime], description: Option[String], isFinished: Boolean)

case class Coordinate(latitude:Double, longitude:Double) extends CoordinateT {
  override def getCoordinate: (Double, Double) = (latitude, longitude)
}

case class Address(nameAddress: String, coordinate: Coordinate)

case class Route(startAddress: Address, endAddress: Address)

sealed trait OrderType {
  val description: String
}

case object DELIVERY extends OrderType {
  override val description: String = "week, months of the year"
}
case object OPERATION extends OrderType {
  override val description: String = "but depending on your needs it can also be a good approach:"
}
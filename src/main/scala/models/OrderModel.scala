package models

import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

// This version just has one route
case class Order(_id: ObjectId = new ObjectId(),
                 clientId: String,
                 cyclistId: String,
                 route: Option[Route],
                 price: Option[Double],
                 orderStates: List[OrderState] = List(),
                 payMethod: Option[String],
                 isPaid: Option[Boolean],
                 lastState: Option[OrderState],
                 products: List[Product] = List(),
                 created:Option[String],
                ) {
  def asDomain = OrderDomain(Some(_id.toHexString),
    clientId, cyclistId, route, price, orderStates, payMethod, isPaid, lastState, products,
    created
  )
}

case class OrderDomain(id: Option[String],
                       clientId: String,
                       cyclistId: String,
                       route: Option[Route],
                       price: Option[Double],
                       orderStates: List[OrderState] = List(),
                       payMethod: Option[String],
                       isPaid: Option[Boolean],
                       lastState: Option[OrderState],
                       products: List[Product] = List(),
                       created: Option[String] = Some(DateTime.now(DateTimeZone.UTC).toString),
                      ) {
  def asResource = Order( id.fold(ObjectId.get()){ new ObjectId(_) },
    clientId, cyclistId, route, price, orderStates, payMethod, isPaid, lastState, products,created
  )
}

case class Product(name:String, description: String, photo: Option[String] = None)

sealed trait OrderStateT {
  val orderStateName: String
}

object OrderStateT {

  def decodeOrderStateName(name:String): OrderStateT = {
    name match {
      case "assigned" => Assigned
      case "wayToPickUpOrder" => WayToPickUpOrder
      case "pickedUpOrder" => PickedUpOrder
      case "wayToDeliverOrder" => WayToDeliverOrder
      case "deliveredOrder" => DeliveredOrder
      case _ => throw new Exception("state name don't recognisable")
    }
  }

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

  def decodePayMethod(name: String): PayMethod = {
    name match {
      case "cash" => Cash
      case "card" => Card
      case _ => throw new Exception("payMethod name don't recognisable")
    }
  }

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

case class OrderState(nameState: String, startTime: Option[String], endTime: Option[String], description: Option[String], isFinished: Boolean)

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
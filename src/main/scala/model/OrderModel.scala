package model

import akka.http.scaladsl.model.DateTime
import org.bson.types.ObjectId

case class Order(id:ObjectId = new ObjectId(),
                 finalPrice:Option[Double],
                 isPaid: Option[Boolean],
                 paymentDateTime: Option[DateTime],
                 paymentMethod: Option[String]
                )
//TODO: ask about status order
//TODO: make TypeStatusOrder an Enumeration
case class StatusOrder(name: String,
                       orderType: String,
                       description: String,
                       isCanceled:Option[Boolean],
                       doAction:String)

sealed trait OrderType {
  val description:String
}
case object DELIVERY extends OrderType {
  override val description: String = "week, months of the year"
}
case object OPERATION extends OrderType {
  override val description: String = "but depending on your needs it can also be a good approach:"
}

sealed trait StatusOrderType {
  val description: String
}

case object EMITIDO_CLIENTE extends StatusOrderType {
  override val description: String = "Emitido por el cliente"
}

case object RECHAZADO_ADMINISTRADO extends StatusOrderType {
  override val description: String =  "La order fue rechazada por el administrador "
}



// val orderTypes:List[_ <: OrderType] = DELIVERY :: OPERATION :: Nil


/*
* // a "heavier"
package com.acme.app {
    sealed trait Margin
    case object TOP extends Margin
    case object RIGHT extends Margin
    case object BOTTOM extends Margin
    case object LEFT extends Margin
}
* */
package model

import akka.http.scaladsl.model.DateTime
import org.bson.types.ObjectId

case class Order(id:ObjectId = new ObjectId(),
                 orderType: OrderType,
                 creationTime: String,
                 statusFromCyclist: StatusFromCyclist,
                 finalPrice:Option[Double],
                 isPaid: Option[Boolean],
                 paymentDay: Option[String],
                 paymentMethod: Option[DateTime]
                )
//TODO: ask about status order
case class OrderType(name: String, description: String)
case class StatusFromCyclist(timeAccepted:Option[String], timeDeclined:Option[String])
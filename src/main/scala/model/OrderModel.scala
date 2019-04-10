package model

import org.bson.types.ObjectId

case class Order(id:ObjectId = new ObjectId(),
                 orderType: OrderType,
                 creationTime: String,
                )
//TODO: ask about status order
case class OrderType(name: String, description: String)
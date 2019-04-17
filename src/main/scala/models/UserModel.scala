package models

import org.bson.types.ObjectId

trait Person {
  val firstName: String
  val lastName: String
  val dni: String
}

case class Device(name:String, number:String, imei:String, token: Option[String])

case class UserProducer(nameCompany:String, address: String, phone:String, ruc: String)

case class UserCyclist(firstName: String, lastName: String, dni: String) extends Person


case class User(id: ObjectId = new ObjectId(),
                device: Device,
                userCyclist: Option[UserCyclist],
                userProducer: Option[UserProducer],
                email: String,
                password: String,
                isAuthenticated: Boolean,
                orders: Option[List[Order]])


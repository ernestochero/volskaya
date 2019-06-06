package models

import org.bson.types.ObjectId

trait Person {
  val firstName: String
  val lastName: String
  val dni: String
}

case class Device(name:String, number:String, imei:String, token: Option[String] = None)

case class UserProducer(nameCompany:String, address: String, phone:String, ruc: String)

case class UserCyclist(firstName: String, lastName: String, dni: String) extends Person


case class UserDomain(id: Option[String],
                device: Option[Device],
                userCyclist: Option[UserCyclist],
                userProducer: Option[UserProducer],
                email: Option[String],
                password: Option[String],
                isAuthenticated: Option[Boolean],
                orders: Option[List[Order]]) {
  def asResource = User( id.fold(ObjectId.get()){new ObjectId(_)},
    device, userCyclist, userProducer, email, password, isAuthenticated, orders)
}

case class User(_id: ObjectId = new ObjectId(),
                        device: Option[Device] = None,
                        userCyclist: Option[UserCyclist] = None,
                        userProducer: Option[UserProducer] = None,
                        email: Option[String] = None,
                        password: Option[String] = None,
                        isAuthenticated: Option[Boolean] = None,
                        orders: Option[List[Order]] = None) {
  def asDomain = UserDomain(Some(_id.toHexString),device, userCyclist, userProducer, email, password, isAuthenticated, orders)
}
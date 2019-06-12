package models

import org.bson.types.ObjectId

case class Device(name:String, number:String, imei:String, token: Option[String] = None)

case class PersonalInformation(firstName: String, lastName: String, dni: String)

case class FavoriteSite(coordinate: Coordinate, name: String, address: String)

case class UserDomain(id: Option[String],
                      device: Option[Device],
                      personalInformation: Option[PersonalInformation],
                      email: Option[String],
                      password: Option[String],
                      isAuthenticated: Option[Boolean],
                      orders: Option[List[Order]],
                      favoriteSites: Option[List[FavoriteSite]],
                      confirmationCode: Option[String]
                     ) {
  def asResource = User( id.fold(ObjectId.get()){new ObjectId(_)},
    device, personalInformation, email, password, isAuthenticated, orders, favoriteSites, confirmationCode)
}

case class User(_id: ObjectId = new ObjectId(),
                device: Option[Device] = None,
                personalInformation: Option[PersonalInformation] = None,
                email: Option[String] = None,
                password: Option[String] = None,
                isAuthenticated: Option[Boolean] = None,
                orders: Option[List[Order]] = None,
                favoriteSites: Option[List[FavoriteSite]] = None,
                confirmationCode: Option[String] = None
               ) {
  def asDomain = UserDomain(Some(_id.toHexString),device, personalInformation, email, password, isAuthenticated, orders, favoriteSites, confirmationCode)
}
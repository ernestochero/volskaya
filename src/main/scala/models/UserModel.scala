package models

import org.bson.types.ObjectId
import sangria.execution.UserFacingError

object OrderManagementMessages {

  case class GetAllOrders(limit: Int, offset: Int)

  case class SaveOrder(order: Order)

  case class GetOrder(id: ObjectId)
}

object UserManagementMessages {

  case class SaveUser(user: User)

  case class VerifyLogin(email: String, password: String)

  case class GetUser(id: ObjectId)

  case class GetAllUsers(limit: Int, offset: Int)

  case class UpdateEmail(id: ObjectId, email: String)

  case class UpdatePassword(id: ObjectId, oldPassword: String, newPassword: String)

  case class UpdatePersonalInformation(id: ObjectId, personalInformation: PersonalInformation)

  case class AddFavoriteSite(id: ObjectId, favoriteSite: FavoriteSite)

  case class SaveVerificationCode(id: ObjectId, verificationCode: String)

  case class CheckCode(id: ObjectId, code: String)

  case class SendVerificationCode(verificationCode: String, phoneNumber: String)

  case class CalculatePriceRoute(coordinateStart: Coordinate, coordinateFinish: Coordinate)

}

trait UserStorageResponse
case class UserSuccessResponse(userId: String) extends UserStorageResponse

object UserManagementExceptions {
  case class UserNotFoundException(message: String) extends Exception with UserFacingError {
    override def getMessage: String = message
  }

  case class MatchPatternNotFoundException(message: String) extends Exception with UserFacingError {
    override def getMessage: String = message
  }

  case class SendVerificationCodeException(message: String) extends Exception with UserFacingError {
    override def getMessage: String = message
  }

  case class CalculatePriceRouteException(message: String, error: String = "01")
      extends Exception
      with UserFacingError {
    override def getMessage: String = message
  }

  case class VolskayaAPIException(message: String) extends Throwable

}

case class Device(name: String, number: String, imei: String, token: Option[String] = None)

case class PersonalInformation(firstName: String, lastName: String, dni: String)

case class FavoriteSite(coordinate: Coordinate, name: String, address: String)

case class UserDomain(id: Option[String],
                      device: Option[Device],
                      personalInformation: Option[PersonalInformation],
                      email: Option[String],
                      password: Option[String],
                      isAuthenticated: Option[Boolean],
                      favoriteSites: Option[List[FavoriteSite]],
                      confirmationCode: Option[String],
                      role: Option[String]) {
  def asResource =
    User(id.fold(ObjectId.get()) { new ObjectId(_) },
         device,
         personalInformation,
         email,
         password,
         isAuthenticated,
         favoriteSites,
         confirmationCode,
         role)
}

case class User(_id: ObjectId = new ObjectId(),
                device: Option[Device] = None,
                personalInformation: Option[PersonalInformation] = None,
                email: Option[String] = None,
                password: Option[String] = None,
                isAuthenticated: Option[Boolean] = None,
                favoriteSites: Option[List[FavoriteSite]] = None,
                confirmationCode: Option[String] = None,
                role: Option[String] = None) {
  def asDomain =
    UserDomain(Some(_id.toHexString),
               device,
               personalInformation,
               email,
               password,
               isAuthenticated,
               favoriteSites,
               confirmationCode,
               role)
}

sealed trait Role {
  val roleName: String
}

object Role {

  def decodeRoleName(name: String) =
    name match {
      case "client"  => Client
      case "cyclist" => Cyclist
      case _         => throw new Exception("role name don't recognisable")
    }

  case object Client extends Role {
    override val roleName: String = "client"
  }

  case object Cyclist extends Role {
    override val roleName: String = "cyclist"
  }

}

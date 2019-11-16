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
  case class VolskayaAPIException(message: String) extends Exception
}

case class Device(name: Option[String] = None,
                  number: String,
                  imei: Option[String] = None,
                  token: Option[String] = None)
case class PersonalInformation(firstName: String, lastName: String, dni: String)
case class CompanyInformation(name: String, address: String, ruc: String)
case class UserAuthenticate(verificationCode: Int, isAuthenticated: Boolean = false)
case class FavoriteSite(coordinate: Coordinate, name: String, address: String)

case class User(_id: ObjectId = new ObjectId(),
                device: Option[Device] = None,
                personalInformation: Option[PersonalInformation] = None,
                companyInformation: Option[CompanyInformation] = None,
                userAuthenticate: Option[UserAuthenticate] = None,
                email: Option[String] = None,
                password: Option[String] = None,
                favoriteSites: Option[List[FavoriteSite]] = None,
                role: Option[Role] = None)

// find a better way to implement that ... waiting for PR :https://github.com/mongodb/mongo-scala-driver/pull/69
sealed trait Role
object Role {
  case object DeliveryPerson  extends Role
  case object DeliveryCompany extends Role
  case object Client          extends Role
  case object Company         extends Role

  def decodeRoleName(name: String): Role =
    name match {
      case "Client"          => Client
      case "DeliveryCompany" => DeliveryCompany
      case "DeliveryPerson"  => DeliveryPerson
      case "Company"         => Company
      case _                 => throw new Exception("role name don't recognisable")
    }
  def encodeRoleName(role: Role): String = role.toString
}

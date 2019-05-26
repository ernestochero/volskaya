package models

abstract class FieldId(val name:String) extends Serializable {
  override def toString: String = name
}

case object PasswordField extends FieldId("password")
case object EmailField extends FieldId("email")
case object UserProducerField extends FieldId("userProducer")
case object UserCyclistField extends FieldId("userCyclist")
case object UserFieldId extends FieldId("user")
case object DefaultFieldId extends FieldId("")

object VolskayaMessages {

  sealed trait VolskayaMessage {
    def message: String
  }

  sealed trait VolskayaResponse extends VolskayaMessage {
    val responseCode: String
    val responseMessage: String
    override def message: String = s"ResponseCode ->  $responseCode with message -> $responseMessage"
  }

  case class VolskayaIncorrectParameters(responseCode: String = "11", responseMessage: String = "Incorrect Parameters") extends VolskayaResponse

  case class VolskayaSuccessfulResponse(responseCode: String = "00", fieldId: FieldId) extends VolskayaResponse {
    override val responseMessage = s"The ${fieldId.name} was updated successfully"
  }

  case class VolskayaFailedResponse(responseCode: String = "01", fieldId: FieldId) extends VolskayaResponse {
    override val responseMessage = s"Failed to update the ${fieldId.name}"
  }

  case class VolskayaSuccessfulLogin(responseCode: String = "20") extends VolskayaResponse {
    override val responseMessage: String = s"The user Logged successfully"
  }

  case class VolskayaFailedLogin(responseCode: String = "21") extends VolskayaResponse {
    override val responseMessage: String = s"Failed to Login"
  }

  case  class VolskayaUserNotExist(responseCode: String = "22",  errorMsg:String) extends VolskayaResponse {
    override val responseMessage: String = s"The User Doesn't Exist : $errorMsg"
  }

  case class VolskayaDefaultErrorMessage(responseCode: String = "22", errorMsg:String) extends VolskayaResponse {
    override val responseMessage: String = s"An unsuspected error happened : $errorMsg"
  }

  case class VolskayaSuccessfulPrice(responseCode: String = "00") extends VolskayaResponse {
    override val responseMessage: String = s"The Price Calculated Correctly"
  }

  case class VolskayaSuccessfulUser(responseCode: String = "00") extends VolskayaResponse {
    override val responseMessage: String = s"User extracted correctly"
  }

  case class VolskayaGetPriceResponse(price: Double, volskayaResponse: VolskayaResponse)

  case class VolskayaGetUserResponse(userDomain: UserDomain, volskayaResponse: VolskayaResponse)

}
package models

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg

abstract class FieldId(val name:String) extends Serializable {
  override def toString: String = name
}

case object PasswordField extends FieldId("password")
case object EmailField extends FieldId("email")
case object PersonalInformationFieldId extends FieldId("personalInformation")
case object UserField extends FieldId("user")
case object PriceFieldId extends FieldId("price")
case object DistanceFieldId extends FieldId("distance")
case object FavoriteSiteFieldId extends FieldId("favoriteSite")
case object VerificationCodeId extends FieldId("verificationCode")
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


  def getSuccessUpdateMessage(fieldId: FieldId): String = s"The ${fieldId.name} was updated successfully"
  def getSuccessLoginMessage(fieldId: FieldId): String = s"The ${fieldId.name} was logged successfully"
  def getSuccessCalculateMessage(fieldId: FieldId): String = s"The ${fieldId.name} was calculate successfully"
  def getSuccessGetMessage(fieldId: FieldId): String = s"The ${fieldId.name} was extracted successfully"
  def getSuccessRegisteredMessage(fieldId: FieldId): String = s"The ${fieldId.name} was registered successfully"
  def getSuccessSendcode(fieldId: FieldId): String = s"The ${fieldId.name} was sent successfully"

  def getFailedUpdateMessage(fieldId: FieldId): String = s"The ${fieldId.name} was updated unsuccessfully"
  def getFailedLoginMessage(fieldId: FieldId): String = s"The ${fieldId.name} was logged unsuccessfully"
  def getFailedCalculateMessage(fieldId: FieldId): String = s"The ${fieldId.name} was calculate unsuccessfully"
  def getFailedGetMessage(fieldId: FieldId): String = s"The ${fieldId.name} was extracted unsuccessfully"

  def getDefaultErrorMessage(errorMsg:String): String = s"An unsuspected error happened : $errorMsg"
  def getUserNotExistMessage: String = "The User Doesn't Exist"

  case class VolskayaIncorrectParameters(responseCode: String = "11", responseMessage: String = "Incorrect Parameters") extends VolskayaResponse

  case class VolskayaSuccessResponse(responseCode: String = "00", responseMessage: String) extends VolskayaResponse

  case class VolskayaFailedResponse(responseCode: String = "01", responseMessage: String) extends VolskayaResponse

  case class VolskayaGetPriceResponse(price: Option[Double], volskayaResponse: VolskayaResponse)

  case class VolskayaGetUserResponse(userDomain: Option[UserDomain], volskayaResponse: VolskayaResponse)

  case class VolskayaLoginResponse(id:Option[String], volskayaResponse: VolskayaResponse)

  case class VolskayaRegisterResponse(id: Option[String], volskayaResponse: VolskayaResponse)

}
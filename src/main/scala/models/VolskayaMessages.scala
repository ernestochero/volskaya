package models

abstract class FieldId(val name:String) extends Serializable {
  override def toString: String = name
}

case object PasswordField extends FieldId("password")
case object EmailField extends FieldId("email")
case object UserProducerField extends FieldId("userProducer")
case object UserCyclistField extends FieldId("userCyclist")


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

  case class VolskayaFailedResponse(responseCode: String = "05", fieldId: FieldId) extends VolskayaResponse {
    override val responseMessage = s"Failed to update the ${fieldId.name}"
  }

}
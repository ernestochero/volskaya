package googlefcmservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethod, HttpMethods, RequestEntity}
import googleMapsService.{ContextFCM, Request}
import googlefcmservice.model._
import play.api.libs.json._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable



trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val ResultNotificationFormat = jsonFormat1(ResultNotification)
  implicit val SendNotificationFormat = jsonFormat5(SendNotification)
}

object SendNotificationApi extends JsonSupport {

  def sendNotificationCode()(implicit contextFCM: ContextFCM) = {
    val request = new SendNotificationApiRequest(contextFCM)
    request.makeRequest(Map.empty[String, String])
  }

}

case class SendNotificationApiRequest(context: ContextFCM) extends  Request[SendNotification] {
  val sms = "hey test"
  val phoneNumber = "987913771"
  val input:JsValue = Json.obj("to" -> context.to, "data" -> Json.obj("sms" -> sms, "numero" -> phoneNumber))

  def authorization: Authorization = Authorization(BasicHttpCredentials("key",context.key))
  override def method: HttpMethod = HttpMethods.POST
  override def entity = HttpEntity(ContentTypes.`application/json`, Json.stringify(input))
  override def baseUri: String = {
    "https://fcm.googleapis.com/fcm/send"
  }

  override def headers: immutable.Seq[HttpHeader] = List(authorization)


}
package googlefcmservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethod, HttpMethods, RequestEntity}
import googleMapsService.{ContextFCM, Request}
import googlefcmservice.model._
import play.api.libs.json._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable
import scala.concurrent.Future



trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val ResultNotificationFormat = jsonFormat1(ResultNotification)
  implicit val SendNotificationFormat = jsonFormat5(SendNotification)
}

object SendNotificationApi extends JsonSupport {

  def sendNotificationCode(code:String, phoneNumber:String)(implicit contextFCM: ContextFCM): Future[SendNotification] = {
    val request = SendNotificationApiRequest(contextFCM, code, phoneNumber)
    request.makeRequest(Map.empty[String, String])
  }

}

case class SendNotificationApiRequest(context: ContextFCM, code: String, phoneNumber: String) extends  Request[SendNotification] {
  override def method: HttpMethod = HttpMethods.POST
  override def entity = HttpEntity(ContentTypes.`application/json`, Json.stringify(buildMessage))
  override def baseUri: String = {
    "https://fcm.googleapis.com/fcm/send"
  }
  val buildMessage: JsValue =  Json.obj("to" -> context.to, "data" -> Json.obj("sms" -> code, "numero" -> phoneNumber))
  val headerAuthorization = RawHeader("authorization", context.token)
  override def headers: immutable.Seq[HttpHeader] = List(headerAuthorization)

}
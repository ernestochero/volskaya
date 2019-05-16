package googleMapsService

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer

import scala.concurrent.Future


trait Request[M] {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val BASE_URI = "https://maps.googleapis.com/maps/api"
  var params = Map[String, String]()

  def makeRequest()(implicit um:Unmarshaller[ResponseEntity, M]): Future[M] = {
    val request = buildRequest()
    fetch(request)
  }

  def buildRequest() = {
    params("key", key)

    HttpRequest(
      uri = buildUri().toString(),
      method = HttpMethods.GET
    )
  }

  def fetch(request: HttpRequest)(implicit um: Unmarshaller[ResponseEntity, M]): Future[M] = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(request)

    responseFuture.flatMap { response =>
      Unmarshal(response.entity).to[M]
    }
  }

  def validateRequest() = {}

  def uri(): String = { "" }
  def key(): String = { "" }

  def buildUri() = {
    Uri(BASE_URI + uri).withQuery(buildQuery())
  }

  def buildQuery() = {
    Query(params)
  }

  def params(key: String, value: String) {
    params += (key -> value)
  }

}

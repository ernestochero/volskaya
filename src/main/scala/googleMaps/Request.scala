package googleMaps

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, Unmarshaller }
import akka.stream.ActorMaterializer

import scala.collection.immutable
import scala.concurrent.Future

trait Request[M] {

  implicit val system           = ActorSystem()
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def makeRequest(
    params: Map[String, String]
  )(implicit um: Unmarshaller[ResponseEntity, M]): Future[M] = {
    val request = buildRequest(params)
    fetch(request)
  }

  def buildRequest(params: Map[String, String]): HttpRequest =
    HttpRequest(
      uri = buildUri(params).toString(),
      method = method,
      headers = headers,
      entity = entity
    )

  def fetch(request: HttpRequest)(implicit um: Unmarshaller[ResponseEntity, M]): Future[M] = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(request)

    responseFuture.flatMap { response =>
      Unmarshal(response.entity).to[M]
    }
  }

  def validateRequest() = {}

  def uri(): String                      = ""
  def method: HttpMethod                 = HttpMethods.GET
  def entity                             = HttpEntity.Empty
  def baseUri: String                    = ""
  def headers: immutable.Seq[HttpHeader] = Nil

  def buildUri(params: Map[String, String]): Uri =
    Uri(baseUri + uri).withQuery(buildQuery(params))

  def buildQuery(params: Map[String, String]): Query =
    Query(params)

}

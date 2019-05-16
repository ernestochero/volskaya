package googleMapsService

// https://developers.google.com/maps/documentation/distance-matrix/start
// https://developers.google.com/maps/documentation/distance-matrix/intro#DistanceMatrixRequests

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import model.Avoid.Avoid
import model.TravelMode.TravelMode
import model.Units.Units
import model.Language.Language
import model.TransitMode.TransitMode
import model.TrafficModel.TrafficModel
import model.TransitRoutingPreference.TransitRoutingPreference
import model._
import spray.json.DefaultJsonProtocol


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol{
  implicit val DistanceFormat = jsonFormat2(Distance)
  implicit val DurationFormat = jsonFormat2(Duration)
  implicit val ElementFormat = jsonFormat2(DistanceMatrixElement)
  implicit val RowFormat = jsonFormat1(DistanceMatrixRow)
  implicit val DistanceMatrixFormat = jsonFormat4(DistanceMatrix)
}

object DistanceMatrixApi extends JsonSupport {

  def getDistanceMatrix(origins: List[String],
                        destinations: List[String],
                        units: Option[Units],
                        mode: Option[TravelMode] = None,
                        language: Option[Language] = None,
                        arrivalTime: Option[Int] = None,
                        departureTime: Option[Either[Int, String]] = None,
                        region: Option[String] = None,
                        avoid: Option[Avoid] = None,
                        trafficModel: Option[TrafficModel] = None,
                        transitMode: Option[List[TransitMode]] = None,
                        transitRoutingPreference: Option[TransitRoutingPreference] = None
                       )(implicit context: Context) = {

    val request = new DistanceMatrixApiRequest(context)

    request.params("origins", origins.mkString("|"))
    request.params("destinations", destinations.mkString("|"))

    units match {
      case Some(value) => request.params("units", value.toString)
      case None => {}
    }

    mode match {
      case Some(value) => request.params("mode", value.toString)
      case None => {}
    }

    language match {
      case Some(value) => request.params("language", value.toString)
      case None => {}
    }

    // integer in seconds since midnight, January 1, 1970 UTC
    // can online specify one of arrival_time or departure_time
    arrivalTime match {
      case Some(value) => request.params("arrival_time", value.toString())
      case None => {}
    }

    // integer in seconds since midnight, January 1, 1970 UTC or "now"
    departureTime match {
      case Some(Left(value)) => request.params("departure_time", value.toString())
      case Some(Right(value)) => request.params("departure_time", value)
      case None => {}
    }

    region match {
      case Some(value) => request.params("region", value.toString())
      case None => {}
    }

    avoid match {
      case Some(value) => request.params("avoid", value.toString())
      case None => {}
    }

    trafficModel match {
      case Some(value) => request.params("traffic_model", value.toString())
      case None => {}
    }

    transitMode match {
      case Some(value) => request.params("transit_mode", value.mkString("|"))
      case None => {}
    }

    transitRoutingPreference match {
      case Some(value) => request.params("transit_routing_preference", value.toString())
      case None => {}
    }

    request.makeRequest()

  }
}

case class DistanceMatrixApiRequest(context: Context) extends Request[DistanceMatrix] {
  override def uri(): String = {
    "/distancematrix/json"
  }

  override def key(): String = {
    context.apiKey
  }

}
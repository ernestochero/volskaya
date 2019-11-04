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

import scala.concurrent.Future

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val DistanceFormat       = jsonFormat2(Distance)
  implicit val DurationFormat       = jsonFormat2(Duration)
  implicit val ElementFormat        = jsonFormat2(DistanceMatrixElement)
  implicit val RowFormat            = jsonFormat1(DistanceMatrixRow)
  implicit val DistanceMatrixFormat = jsonFormat4(DistanceMatrix)
}

object DistanceMatrixApi extends JsonSupport {

  def getDistanceMatrix(
    origins: List[String],
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
  )(implicit context: GoogleMapsContext): Future[DistanceMatrix] = {

    val request = DistanceMatrixApiRequest(context)

    val originsParam      = Map("origins"      -> origins.mkString("|"))
    val destinationsParam = Map("destinations" -> destinations.mkString("|"))

    val unitsParam = units match {
      case Some(value) => Map("units" -> value.toString)
      case None        => Map.empty[String, String]
    }

    val modeParam = mode match {
      case Some(value) => Map("mode" -> value.toString)
      case None        => Map.empty[String, String]
    }

    val languageParam = language match {
      case Some(value) => Map("language" -> value.toString)
      case None        => Map.empty[String, String]
    }

    // integer in seconds since midnight, January 1, 1970 UTC
    // can online specify one of arrival_time or departure_time
    val arrivalTimeParam = arrivalTime match {
      case Some(value) => Map("arrival_time" -> value.toString())
      case None        => Map.empty[String, String]
    }

    // integer in seconds since midnight, January 1, 1970 UTC or "now"
    val departureTimeParam = departureTime match {
      case Some(Left(value))  => Map("departure_time" -> value.toString())
      case Some(Right(value)) => Map("departure_time" -> value)
      case None               => Map.empty[String, String]
    }

    val regionParam = region match {
      case Some(value) => Map("region" -> value.toString())
      case None        => Map.empty[String, String]
    }

    val avoidParam = avoid match {
      case Some(value) => Map("avoid" -> value.toString())
      case None        => Map.empty[String, String]
    }

    val trafficModelParam = trafficModel match {
      case Some(value) => Map("traffic_model" -> value.toString())
      case None        => Map.empty[String, String]
    }

    val transitModeParam = transitMode match {
      case Some(value) => Map("transit_mode" -> value.mkString("|"))
      case None        => Map.empty[String, String]
    }

    val transitRoutingPreferenceParam = transitRoutingPreference match {
      case Some(value) => Map("transit_routing_preference" -> value.toString())
      case None        => Map.empty[String, String]
    }

    val params =
    (originsParam ++ destinationsParam
    ++ unitsParam ++ modeParam ++ languageParam
    ++ arrivalTimeParam ++ departureTimeParam ++ regionParam
    ++ avoidParam ++ trafficModelParam ++ transitModeParam ++ transitRoutingPreferenceParam)

    request.makeRequest(params ++ Map("key" -> request.key))

  }
}

case class DistanceMatrixApiRequest(context: GoogleMapsContext) extends Request[DistanceMatrix] {

  val key: String = { context.apiKey }

  override def uri(): String =
    "/distancematrix/json"

  override def baseUri: String =
    "https://maps.googleapis.com/maps/api"

}

package googleMapsService

import googleMapsService.model.Language
import googleMapsService.model.Units
import googleMapsService.model.TravelMode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object test extends App {
  override def main(args: Array[String]): Unit = {
    println("it's a test")
    val context = Context(apiKey = "AIzaSyCXK3faSiD-RBShPD2TK1z1pRRpRaBdYtg")
    val origins = List("-8.121723,-79.036351")
    val destinations = List("-8.107119,-79.022779")
    val language = Language.es
    val units = Units.metric
    val distanceMatrixApi = DistanceMatrixApi.getDistanceMatrix(
      origins = origins,
      destinations = destinations,
      language = Some(language),
      units = Some(units),
      mode = Some(TravelMode.driving)
    )(context)

    distanceMatrixApi.onComplete {
      case Success(value) =>
        println(value)
      case Failure(exception) =>
        println(exception.getMessage)
    }

  }
}
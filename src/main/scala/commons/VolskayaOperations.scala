package commons

import models.Coordinate
import PolygonUtils._
import googleMapsService.model.DistanceMatrix

object VolskayaOperations {
  def getPriceByDistance(distanceMeters: Int): Double = distanceMeters match {
    case v if v > 0 && v <= 1599     => 4.0
    case v if v >= 1600 && v <= 3099 => 5.0
    case v if v >= 3100 && v <= 4099 => 6.0
    case v if v >= 4100 && v <= 5099 => 7.0
    case v if v >= 5100 && v <= 6099 => 8.0
    case v if v >= 6100 && v <= 7099 => 9.0
    case v if v >= 7100 && v <= 8099 => 10.0
    case _                           => 12.0
  }

  def validateCoordinateIntoArea(coordinate: Coordinate): Boolean = inside(coordinate)
  def calculateApproximateTime(distance: Int): Double             = Math.round(distance / 250.0).toDouble
  def calculateDistanceInKilometers(distance: Int): Double = {
    val x = distance / 1000.toDouble
    Math.floor(x * 10) / 10.0
  }
  def calculateCO2Saved(distance: Int): Double = {
    val x = (distance * 83.71847) / 1000
    Math.round(x * 100) / 100.0
  }
  def isDistanceZero(distance: Option[Int]): Boolean      = distance.getOrElse(0) == 0
  def isDistanceOverLimit(distance: Option[Int]): Boolean = distance.getOrElse(0) > 10000
  def extractDistanceFromDistanceMatrix(distanceMatrix: DistanceMatrix): Option[Int] =
    distanceMatrix.status match {
      case "OK" =>
        val result = for {
          row     <- distanceMatrix.rows
          element <- row.elements
        } yield element.distance.value
        result.headOption
      case _ => None
    }
}

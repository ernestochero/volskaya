package commons

import models.Coordinate
import PolygonUtils._
import commons.VolskayaOperations.isDistanceZero
import googleMaps.model.DistanceMatrix
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages._
import zio.{ IO, Task, ZIO }

object VolskayaOperations {
  def getPriceByDistance(distanceMeters: Int): Double = distanceMeters match {
    case v if v > 0 && v <= 1599     => 4.0
    case v if v >= 1600 && v <= 3099 => 5.0
    case v if v >= 3100 && v <= 4099 => 7.0
    case v if v >= 4100 && v <= 5099 => 8.0
    case v if v >= 5100 && v <= 6099 => 10.0
    case v if v >= 6100 && v <= 7099 => 11.0
    case v if v >= 7100 && v <= 8099 => 13.0
    case v if v >= 8100 && v <= 9099 => 14.0
    case _                           => 15.0
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
  def isDistanceZero(distance: Int): Boolean      = distance == 0
  def isDistanceOverLimit(distance: Int): Boolean = distance > 10000
  def extractDistanceFromDistanceMatrix(
    distanceMatrix: DistanceMatrix
  ): ZIO[Any, VolskayaAPIException, Int] =
    ZIO
      .fromOption(
        distanceMatrix.status match {
          case "OK" =>
            val result = for {
              row     <- distanceMatrix.rows
              element <- row.elements
            } yield element.distance.value
            result.headOption
          case _ => None
        }
      )
      .mapError(_ => VolskayaAPIException("distanceMatrix are not defined"))

  def buildGetPriceResponse(
    initialDistance: Int,
    secondDistance: Int
  ): IO[VolskayaAPIException, VolskayaPrice] =
    for {
      _ <- ZIO.when(isDistanceZero(secondDistance))(
        ZIO.fail(VolskayaAPIException("Distance can't be zero"))
      )
      _ <- ZIO.when(isDistanceOverLimit(secondDistance))(
        ZIO.fail(VolskayaAPIException("Route exceeds the limit of 10 kilometers"))
      )
      distanceInKilometers   = calculateDistanceInKilometers(secondDistance)
      approximateTime        = calculateApproximateTime(secondDistance)
      co2Saved               = calculateCO2Saved(secondDistance)
      approximateInitialTime = calculateApproximateTime(initialDistance)
      approximateFinalTime   = approximateTime + approximateInitialTime
      price                  = getPriceByDistance(secondDistance)
    } yield
      VolskayaPrice(
        price,
        distanceInKilometers,
        co2Saved,
        approximateFinalTime,
      )
}

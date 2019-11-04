package graphql

import modules.UserCollectionModule.UserCollection
import models.{ Coordinate, User }
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.{
  VolskayaFailedResponse,
  VolskayaGetPriceResponse,
  VolskayaResultSuccessResponse,
  VolskayaSuccessResponse,
  getSuccessCalculateMessage
}
import modules.GoogleMapsModule.GoogleMapsService
import zio.{ Queue, Ref, UIO, ZIO }
import zio.console.Console
import zio.stream.ZStream
import commons.VolskayaOperations._
import commons.Constants._
import googleMapsService.model.DistanceMatrix
case class VolskayaService(userCollection: Ref[UserCollection],
                           googleMapsService: Ref[GoogleMapsService],
                           subscribers: Ref[List[Queue[String]]]) {

  def wakeUpVolskaya: UIO[String] = UIO.succeed("I'm awake")

  def getAllUsers(
    limit: Int,
    offset: Int
  ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[List, User]] =
    userCollection.get.flatMap(
      _.getAllUsers(limit, offset)
    )

  def getUser(
    id: String
  ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option, User]] =
    userCollection.get.flatMap(
      _.getUser(id)
    )

  def updatePassword(
    id: String,
    oldPassword: String,
    newPassword: String
  ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option, String]] =
    userCollection.get.flatMap(
      _.updatePassword(id, oldPassword, newPassword)
    )

  def calculatePriceRoute(
    coordinateStart: Coordinate,
    coordinateFinish: Coordinate
  ): ZIO[Console, VolskayaAPIException, VolskayaGetPriceResponse] = {
    def buildGetPriceResponse(initialDistance: Option[Int],
                              secondDistance: Option[Int]): VolskayaGetPriceResponse = {
      val distanceInKilometers   = secondDistance.map(calculateDistanceInKilometers)
      val approximateTime        = secondDistance.map(calculateApproximateTime)
      val co2Saved               = secondDistance.map(calculateCO2Saved)
      val approximateInitialTime = initialDistance.map(calculateApproximateTime)

      if (isDistanceZero(secondDistance)) {
        VolskayaGetPriceResponse(
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Distance can't be zero",
            responseCode = "04"
          )
        )
      } else if (isDistanceOverLimit(secondDistance)) {
        VolskayaGetPriceResponse(
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Route exceeds the limit of 10 kilometers",
            responseCode = "03"
          )
        )
      } else {
        val approximateFinalTime = for {
          time        <- approximateTime
          initialTime <- approximateInitialTime
        } yield time + initialTime
        val price = secondDistance.map(getPriceByDistance)
        VolskayaGetPriceResponse(
          price,
          distanceInKilometers,
          co2Saved,
          approximateFinalTime,
          VolskayaSuccessResponse(
            responseMessage = getSuccessCalculateMessage(models.PriceFieldId)
          )
        )
      }
    }
    val priceResponse = (for {
      distanceFastBiciToPickUpLocation <- googleMapsService.get.flatMap(
        _.calculateDistanceMatrixFromCoordinates(fastBiciCoordinate, coordinateStart)
      )
      distancePickUptoLeaveLocation <- googleMapsService.get.flatMap(
        _.calculateDistanceMatrixFromCoordinates(coordinateStart, coordinateFinish)
      )
      initialDistance = extractDistanceFromDistanceMatrix(distanceFastBiciToPickUpLocation)
      secondDistance  = extractDistanceFromDistanceMatrix(distancePickUptoLeaveLocation)
      priceResponse = if (initialDistance.isDefined && secondDistance.isDefined) {
        buildGetPriceResponse(initialDistance, secondDistance)
      } else {
        VolskayaGetPriceResponse(
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Distances are not defined",
            responseCode = "05"
          )
        )
      }
    } yield priceResponse).mapError(e => VolskayaAPIException(e.getMessage))

    for {
      result <- if (validateCoordinateIntoArea(coordinateStart) && validateCoordinateIntoArea(
                      coordinateFinish
                    )) { priceResponse } else
        ZIO.succeed(
          VolskayaGetPriceResponse(
            volskayaResponse = VolskayaFailedResponse(
              responseMessage = "Delivery out of range",
              responseCode = "02"
            )
          )
        )
    } yield result
  }

  def userAddedEvent: ZStream[Any, Nothing, String] = ZStream.unwrap {
    for {
      queue <- Queue.unbounded[String]
      _     <- subscribers.update(queue :: _)
    } yield ZStream.fromQueue(queue)
  }
}

object VolskayaService {
  def make(userCollection: UserCollection,
           googleMapsService: GoogleMapsService): UIO[VolskayaService] =
    for {
      user        <- Ref.make(userCollection)
      googleMaps  <- Ref.make(googleMapsService)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new VolskayaService(user, googleMaps, subscribers)
}

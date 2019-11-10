package graphql

import caliban.CalibanError.ExecutionError
import modules.UserCollectionModule.UserCollection
import models.{ Coordinate, Role, User }
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.{
  VolskayaFailedResponse,
  VolskayaPrice,
  VolskayaResult,
  VolskayaSuccessResponse,
  getSuccessCalculateMessage
}
import modules.GoogleMapsModule.GoogleMapsService
import zio.{ Queue, Ref, UIO, ZIO }
import zio.console.Console
import zio.stream.ZStream
import commons.VolskayaOperations._
import commons.Constants._
import commons.Logger._
import modules.LoggingModule
case class VolskayaService(userCollection: Ref[UserCollection],
                           googleMapsService: Ref[GoogleMapsService],
                           subscribers: Ref[List[Queue[String]]]) {

  def wakeUpVolskaya: UIO[String] = UIO.succeed("I'm awake")

  def getAllUsers(
    limit: Int,
    offset: Int
  ): ZIO[Console, ExecutionError, VolskayaResult[List, User]] =
    userCollection.get
      .flatMap(
        _.getAllUsers(limit, offset)
      )
      .mapError {
        case ex: VolskayaAPIException => ExecutionError(ex.message)
        case other                    => ExecutionError(s"Error $other")
      }

  def getUser(
    id: String
  ): ZIO[Console, ExecutionError, VolskayaResult[Option, User]] =
    userCollection.get
      .flatMap(
        _.getUser(id)
      )
      .mapError {
        case ex: VolskayaAPIException => ExecutionError(ex.message)
        case other                    => ExecutionError(s"Error $other")
      }

  def updatePassword(
    id: String,
    oldPassword: String,
    newPassword: String
  ): ZIO[Console, ExecutionError, VolskayaResult[Option, String]] =
    userCollection.get
      .flatMap(
        _.updatePassword(id, oldPassword, newPassword)
      )
      .mapError {
        case ex: VolskayaAPIException => ExecutionError(ex.message)
        case other                    => ExecutionError(s"Error $other")
      }

  def calculatePriceRoute(
    coordinateStart: Coordinate,
    coordinateFinish: Coordinate
  ): ZIO[Console, ExecutionError, VolskayaResult[Option, VolskayaPrice]] = {
    def buildGetPriceResponse(
      initialDistance: Int,
      secondDistance: Int
    ): VolskayaResult[Option, VolskayaPrice] = {
      val distanceInKilometers   = calculateDistanceInKilometers(secondDistance)
      val approximateTime        = calculateApproximateTime(secondDistance)
      val co2Saved               = calculateCO2Saved(secondDistance)
      val approximateInitialTime = calculateApproximateTime(initialDistance)

      if (isDistanceZero(secondDistance)) {
        VolskayaResult[Option, VolskayaPrice](
          None,
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Distance can't be zero",
            responseCode = "04"
          )
        )
      } else if (isDistanceOverLimit(secondDistance)) {
        VolskayaResult[Option, VolskayaPrice](
          None,
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Route exceeds the limit of 10 kilometers",
            responseCode = "03"
          )
        )
      } else {
        val approximateFinalTime = approximateTime + approximateInitialTime
        val price                = getPriceByDistance(secondDistance)
        VolskayaResult[Option, VolskayaPrice](
          Some(
            VolskayaPrice(
              price,
              distanceInKilometers,
              co2Saved,
              approximateFinalTime,
            )
          ),
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
      initialDistance <- extractDistanceFromDistanceMatrix(distanceFastBiciToPickUpLocation)
      secondDistance  <- extractDistanceFromDistanceMatrix(distancePickUptoLeaveLocation)
      priceResponse = buildGetPriceResponse(initialDistance, secondDistance)
    } yield priceResponse).mapError {
      case ex: VolskayaAPIException => ExecutionError(ex.message)
      case other                    => ExecutionError(s"Error: ${other.getMessage}")
    }

    for {
      result <- if (validateCoordinateIntoArea(coordinateStart) && validateCoordinateIntoArea(
                      coordinateFinish
                    )) { priceResponse } else
        ZIO.succeed(
          VolskayaResult[Option, VolskayaPrice](
            None,
            volskayaResponse = VolskayaFailedResponse(
              responseMessage = "Delivery out of range",
              responseCode = "02"
            )
          )
        )
    } yield result
  }

  def insertUser(
    roleToInsert: Role
  ): ZIO[Console, ExecutionError, VolskayaResult[Option, User]] = {
    val user = User(role = Some(roleToInsert))
    userCollection.get.flatMap(_.insertUser(user))
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

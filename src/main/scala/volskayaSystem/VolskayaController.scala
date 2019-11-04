package volskayaSystem

import akka.actor.ActorSystem
import akka.util.Timeout
import commons.{ Constants, PolygonUtils }
import googleMapsService.model.{ DistanceMatrix, TravelMode, Units }
import googlefcmservice.model.SendNotification
import models.OrderStateT._
import models._
import models.UserManagementExceptions._
import models.VolskayaMessages._
import org.joda.time.{ DateTime, DateTimeZone }
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.UpdateResult
import user.{ OrderManagerAPI, UserManagerAPI }
import commons.VolskayaOperations._

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, SECONDS }

case class VolskayaController(system: ActorSystem) {

  val userManagerAPI  = UserManagerAPI(system)
  val orderManagerAPI = OrderManagerAPI(system)

  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, SECONDS))

  val log = system.log

  def getAllUsers(limit: Int, offset: Int): Future[Seq[UserDomain]] =
    userManagerAPI.getAllUsers(limit: Int, offset: Int).map(users => users.map(_.asDomain))

  def wakeUpHeroku(): Future[String] =
    Future.successful("I'm awake")

  def getUser(id: String): Future[VolskayaGetUserResponse] =
    userManagerAPI
      .getUser(id)
      .flatMap {
        case Some(user: User) =>
          Future.successful(
            VolskayaGetUserResponse(
              Some(user.asDomain),
              VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField))
            )
          )
        case None =>
          Future.failed(UserNotFoundException(getUserNotExistMessage))
      }
      .recoverWith {
        case ex =>
          val failedResponse =
            VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
          Future.successful(VolskayaGetUserResponse(None, failedResponse))
      }

  def updatePassword(id: String,
                     oldPassword: String,
                     newPassword: String): Future[VolskayaResponse] =
    userManagerAPI
      .updatePassword(id, oldPassword, newPassword)
      .flatMap {
        case res: UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
          Future.successful(
            VolskayaSuccessResponse(
              responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)
            )
          )
        case _ =>
          Future.failed(UserNotFoundException(getUserNotExistMessage))
      }
      .recoverWith {
        case ex =>
          Future.successful(
            VolskayaFailedResponse(
              responseMessage = getDefaultErrorMessage(errorMsg = ex.getMessage)
            )
          )
      }

  def verifyLogin(email: String, password: String): Future[VolskayaGetUserResponse] =
    userManagerAPI
      .verifyLogin(email, password)
      .flatMap {
        case Some(user: User) =>
          Future.successful(
            VolskayaGetUserResponse(
              Some(user.asDomain),
              VolskayaSuccessResponse(responseMessage = getSuccessLoginMessage(models.UserField))
            )
          )
        case None =>
          Future.failed(UserNotFoundException(getUserNotExistMessage))
      }
      .recoverWith {
        case ex =>
          val failedResponse =
            VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
          Future.successful(VolskayaGetUserResponse(None, failedResponse))
      }

  def checkCode(id: String, code: String): Future[VolskayaResponse] =
    userManagerAPI
      .checkCode(id, code)
      .flatMap {
        case true =>
          Future.successful(
            VolskayaSuccessResponse(
              responseMessage = getSuccessChecked(fieldId = VerificationCodeId)
            )
          )
        case _ =>
          Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
      }
      .recoverWith {
        case ex =>
          Future.successful(
            VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
          )
      }

  def addFavoriteSite(id: String, favoriteSite: FavoriteSite): Future[VolskayaResponse] =
    userManagerAPI
      .addFavoriteSite(id, favoriteSite)
      .flatMap {
        case res: UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
          Future.successful(
            VolskayaSuccessResponse(
              responseMessage = getSuccessUpdateMessage(fieldId = FavoriteSiteFieldId)
            )
          )
        case _ =>
          Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
      }
      .recoverWith {
        case ex =>
          Future.successful(
            VolskayaFailedResponse(
              responseMessage = getDefaultErrorMessage(errorMsg = ex.getMessage)
            )
          )
      }

  def saveVerificationCode(id: ObjectId, verificationCode: String): Future[Boolean] =
    userManagerAPI
      .saveVerificationCode(id, verificationCode)
      .flatMap {
        case res: UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
          log.debug(getSuccessSave(VerificationCodeId))
          Future.successful(true)
        case _ =>
          Future.failed(UserNotFoundException(getUserNotExistMessage))
      }
      .recoverWith {
        case ex =>
          log.error("Failed : {} with error : {}", getFailedSave(VerificationCodeId), ex.getMessage)
          Future.failed(ex)
      }

  def sendVerificationCode(verificationCode: String, phoneNumber: String): Future[Boolean] =
    userManagerAPI
      .sendVerificationCode(verificationCode, phoneNumber)
      .flatMap {
        case sendNotification: SendNotification if sendNotification.success == 1 =>
          log.debug(getSuccessSendCode(VerificationCodeId))
          Future.successful(true)
        case _ =>
          Future.failed(SendVerificationCodeException(getFailedSendVerificationCode))
      }
      .recoverWith {
        case ex =>
          log.error("Failed : {} with error : {}",
                    log.error(getFailedSendCode(VerificationCodeId)),
                    ex.getMessage)
          Future.failed(ex)
      }

  def registerUser(email: String,
                   password: String,
                   phoneNumber: String): Future[VolskayaGetUserResponse] = {
    import scala.util.Random
    lazy val verificationCode = (1 to 6).foldLeft("") { (c, v) =>
      s"$c${Random.nextInt(10)}"
    }
    lazy val msgVerificationCode = s"Tu codigo de verificacion es $verificationCode "
    val device                   = Device(name = "", number = phoneNumber, imei = "")
    val user = User(email = Some(email),
                    password = Some(password),
                    device = Some(device),
                    favoriteSites = Some(List.empty[FavoriteSite]))
    userManagerAPI
      .saveUser(user)
      .flatMap { user =>
        for {
          saveResult <- saveVerificationCode(user._id, verificationCode)
          sendResult <- sendVerificationCode(msgVerificationCode, phoneNumber)
        } yield {
          if (saveResult && sendResult)
            log.debug("Success")
          else
            log.error("Failed")
        }
        Future.successful(
          VolskayaGetUserResponse(
            Some(user.asDomain),
            VolskayaSuccessResponse(responseMessage = getSuccessLoginMessage(models.UserField))
          )
        )
      }
      .recoverWith {
        case ex =>
          val failedResponse =
            VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
          Future.successful(VolskayaGetUserResponse(None, failedResponse))
      }
  }

  def validateCoordinateIntoArea(coordinate: Coordinate): Boolean = PolygonUtils.inside(coordinate)
  def calculateApproximateTime(distance: Int): Double             = Math.round(distance / 250.0).toDouble
  def calculateDistanceInKilometers(distance: Int): Double = {
    val x = distance / 1000.toDouble
    Math.floor(x * 10) / 10.0
  }
  def calculateCO2Saved(distance: Int): Double = {
    val x = (distance * 83.71847) / 1000
    Math.round(x * 100) / 100.0
  }
  def extractDistanceFromDistanceMatrix(distanceMatrix: DistanceMatrix): Future[Option[Int]] =
    distanceMatrix.status match {
      case "OK" =>
        val result = for {
          row     <- distanceMatrix.rows
          element <- row.elements
        } yield element.distance.value
        Future.successful(result.headOption)
      case _ =>
        Future.failed(
          CalculatePriceRouteException(
            message = s"Status: ${distanceMatrix.status} of distanceMatrix is not OK ",
            error = "05"
          )
        )
    }

  def isDistanceZero(distance: Option[Int]): Boolean      = distance.getOrElse(0) == 0
  def isDistanceOverLimit(distance: Option[Int]): Boolean = distance.getOrElse(0) > 10000

  def calculatePriceRoute(coordinateStart: Coordinate,
                          coordinateFinish: Coordinate): Future[VolskayaGetPriceResponse] = {
    def buildGetPriceResponse(initialDistance: Option[Int],
                              secondDistance: Option[Int]): Future[VolskayaGetPriceResponse] = {
      val distanceInKilometers   = secondDistance.map(calculateDistanceInKilometers)
      val approximateTime        = secondDistance.map(calculateApproximateTime)
      val co2Saved               = secondDistance.map(calculateCO2Saved)
      val approximateInitialTime = initialDistance.map(calculateApproximateTime)
      if (isDistanceZero(secondDistance)) {
        Future.failed(
          CalculatePriceRouteException(
            message = "Distance can't be zero",
            error = "04"
          )
        )
      } else if (isDistanceOverLimit(secondDistance)) {
        Future.failed(
          CalculatePriceRouteException(
            message = "Route exceeds the limit of 10 kilometers",
            error = "03"
          )
        )
      } else {
        val approximateFinalTime = for {
          time        <- approximateTime
          initialTime <- approximateInitialTime
        } yield time + initialTime
        val price = secondDistance.map(getPriceByDistance)
        Future.successful(
          VolskayaGetPriceResponse(
            price,
            distanceInKilometers,
            co2Saved,
            approximateFinalTime,
            VolskayaSuccessResponse(
              responseMessage = getSuccessCalculateMessage(models.PriceFieldId)
            )
          )
        )
      }
    }

    if (validateCoordinateIntoArea(coordinateStart) && validateCoordinateIntoArea(coordinateFinish)) {
      val resultPriceResponse = for {
        distanceFastBiciToPickUpLocation <- userManagerAPI.calculateDistanceRoute(
          Constants.fastBiciCoordinate,
          coordinateStart
        )
        distancePickUptoLeaveLocation <- userManagerAPI.calculateDistanceRoute(
          coordinateStart,
          coordinateFinish
        )
        initialDistance <- extractDistanceFromDistanceMatrix(distanceFastBiciToPickUpLocation)
        secondDistance  <- extractDistanceFromDistanceMatrix(distancePickUptoLeaveLocation)
        if initialDistance.isDefined && secondDistance.isDefined
        priceResponse <- buildGetPriceResponse(initialDistance, secondDistance)
      } yield priceResponse

      resultPriceResponse.recoverWith {
        case exception: CalculatePriceRouteException =>
          Future.successful(
            VolskayaGetPriceResponse(
              volskayaResponse = VolskayaFailedResponse(
                responseMessage = exception.getMessage,
                responseCode = exception.error
              )
            )
          )
        case ex =>
          Future.successful(
            VolskayaGetPriceResponse(
              volskayaResponse = VolskayaFailedResponse(
                responseMessage = ex.getMessage
              )
            )
          )
      }
    } else {
      Future.successful(
        VolskayaGetPriceResponse(
          volskayaResponse = VolskayaFailedResponse(
            responseMessage = "Delivery out of range",
            responseCode = "02"
          )
        )
      )
    }
  }

  // orders section
  def getAllOrders(limit: Int, offset: Int): Future[Seq[OrderDomain]] =
    orderManagerAPI.getAllOrders(limit, offset).map(orders => orders.map(_.asDomain))

  def registerOrder(
    route: Route,
    clientId: String,
    finalClient: FinalClient,
    products: Seq[Product],
    price: Double,
    distance: Double,
    generalDescription: String
  ): Future[VolskayaRegisterResponse] = {

    val unAssignedStateBuilt = OrderState(
      nameState = UnAssigned.orderStateName,
      startTime = Some(DateTime.now(DateTimeZone.UTC).toString),
      description = Some("waiting by cyclist"),
      isFinished = false
    )

    val orderBuilt = Order(
      route = Some(route),
      clientId = Some(clientId),
      finalClient = Some(finalClient),
      products = products.toList,
      price = Some(price),
      distance = Some(distance),
      generalDescription = Some(generalDescription),
      isPaid = Some(false),
      orderStates = List(unAssignedStateBuilt),
      lastState = Some(unAssignedStateBuilt)
    )

    orderManagerAPI
      .saveOrder(orderBuilt)
      .flatMap {
        case order: Order =>
          Future.successful(
            VolskayaRegisterResponse(
              Some(order._id.toHexString),
              VolskayaSuccessResponse(responseMessage = getSuccessSave(models.OrderFieldId))
            )
          )
        case _ =>
          Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
      }
      .recoverWith {
        case ex =>
          Future.successful(
            VolskayaRegisterResponse(
              None,
              VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
            )
          )
      }

  }

  def getOrder(id: String): Future[VolskayaGetOrderResponse] =
    orderManagerAPI
      .getOrder(id)
      .flatMap {
        case Some(order) =>
          Future.successful(
            VolskayaGetOrderResponse(
              Some(order.asDomain),
              VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField))
            )
          )
        case _ =>
          Future.failed(UserNotFoundException(getUserNotExistMessage))
      }
      .recoverWith {
        case ex =>
          val failedResponse =
            VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
          Future.successful(VolskayaGetOrderResponse(None, failedResponse))
      }
}

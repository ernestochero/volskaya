package volskayaSystem

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import googleMapsService.model.{DistanceMatrix, TravelMode, Units}
import googlefcmservice.model.SendNotification
import models._
import models.UserManagementExceptions._
import models.VolskayaMessages._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.UpdateResult
import user.{OrderManagerAPI, UserManagerAPI}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class VolskayaController(system: ActorSystem) {

  val userManagerAPI  = UserManagerAPI(system)
  val orderManagerAPI = OrderManagerAPI(system)

  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  val log = system.log

  def getAllUsers(limit:Int, offset:Int): Future[Seq[UserDomain]] =
    userManagerAPI.getAllUsers(limit:Int, offset:Int).map(users => users.map(_.asDomain))

  def getUser(id: String): Future[VolskayaGetUserResponse] = {
    userManagerAPI.getUser(id).flatMap {
      case Some(user:User) =>
        Future.successful(VolskayaGetUserResponse(Some(user.asDomain), VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField))))
      case None =>
        Future.failed(UserNotFoundException(getUserNotExistMessage))
    }.recoverWith {
      case ex =>
        val failedResponse = VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
        Future.successful(VolskayaGetUserResponse(None, failedResponse))
    }
  }

  def updatePassword(id: String, oldPassword: String, newPassword: String): Future[VolskayaResponse] = {
    userManagerAPI.updatePassword(id, oldPassword, newPassword) .flatMap {
      case res:UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)))
      case _ =>
        Future.failed(UserNotFoundException(getUserNotExistMessage))
    }.recoverWith {
      case ex =>
        Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = ex.getMessage)))
    }
  }

  def verifyLogin(email:String, password: String): Future[VolskayaGetUserResponse] = {
    userManagerAPI.verifyLogin(email, password).flatMap {
      case Some(user: User) =>
        Future.successful(VolskayaGetUserResponse(Some(user.asDomain),VolskayaSuccessResponse(responseMessage = getSuccessLoginMessage(models.UserField))))
      case None =>
        Future.failed(UserNotFoundException(getUserNotExistMessage))
    }.recoverWith {
      case ex =>
        val failedResponse = VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
        Future.successful(VolskayaGetUserResponse(None, failedResponse))
    }
  }

  def checkCode(id: String, code: String): Future[VolskayaResponse] = {
    userManagerAPI.checkCode(id, code).flatMap {
      case true =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessChecked(fieldId = VerificationCodeId)))
      case _ =>
        Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
    }.recoverWith {
      case ex =>
        Future.successful(VolskayaFailedResponse(responseMessage =  getDefaultErrorMessage(ex.getMessage)))
    }
  }

  def addFavoriteSite(id: String, favoriteSite: FavoriteSite): Future[VolskayaResponse] = {
    userManagerAPI.addFavoriteSite(id, favoriteSite).flatMap {
      case res:UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessUpdateMessage(fieldId = FavoriteSiteFieldId)))
      case _ =>
        Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
    }.recoverWith {
      case ex =>
        Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = ex.getMessage)))
    }
  }

  def saveVerificationCode(id: ObjectId, verificationCode: String): Future[Boolean] = {
    userManagerAPI.saveVerificationCode(id, verificationCode).flatMap {
      case res:UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
        log.debug(getSuccessSave(VerificationCodeId))
        Future.successful(true)
      case _ =>
        Future.failed(UserNotFoundException(getUserNotExistMessage))
    }.recoverWith {
      case ex =>
        log.error("Failed : {} with error : {}",getFailedSave(VerificationCodeId), ex.getMessage)
        Future.failed(ex)
    }
  }

  def sendVerificationCode(verificationCode: String, phoneNumber: String): Future[Boolean] = {
    userManagerAPI.sendVerificationCode(verificationCode, phoneNumber).flatMap {
      case sendNotification:SendNotification if sendNotification.success == 1 =>
        log.debug(getSuccessSendCode(VerificationCodeId))
        Future.successful(true)
      case _ =>
        Future.failed(SendVerificationCodeException(getFailedSendVerificationCode))
    }.recoverWith {
      case ex =>
        log.error("Failed : {} with error : {}",log.error(getFailedSendCode(VerificationCodeId)), ex.getMessage)
        Future.failed(ex)
    }
  }

  def registerUser(email: String, password: String, phoneNumber: String): Future[VolskayaGetUserResponse] = {
    import scala.util.Random
    lazy val verificationCode = (1 to 6).foldLeft(""){(c,v) => s"$c${Random.nextInt(10)}"}
    lazy val msgVerificationCode = s"Tu codigo de verificacion es $verificationCode "
    val device = Device(name = "", number = phoneNumber, imei = "")
    val user = User(email = Some(email), password = Some(password), device = Some(device), favoriteSites = Some(List.empty[FavoriteSite]))
    userManagerAPI.saveUser(user).flatMap { user =>
      for {
        saveResult <- saveVerificationCode(user._id, verificationCode)
        sendResult <- sendVerificationCode(msgVerificationCode, phoneNumber)
      } yield {
        if (saveResult && sendResult)
          log.debug("Success")
        else
          log.error("Failed")
      }
      Future.successful(VolskayaGetUserResponse(Some(user.asDomain),VolskayaSuccessResponse(responseMessage = getSuccessLoginMessage(models.UserField))))
    }.recoverWith {
      case ex =>
        val failedResponse = VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage))
        Future.successful(VolskayaGetUserResponse(None, failedResponse))
    }
  }

  def calculatePriceRoute(coordinateStart: Coordinate, coordinateFinish: Coordinate): Future[VolskayaGetPriceResponse] = {
    val origins = coordinateStart.toString :: Nil
    val destinations = coordinateFinish.toString :: Nil
    val units = Some(Units.metric)
    val travelMode = Some(TravelMode.walking)
    //TODO optimize this function when we have a geoZone
    def calculateByDistance(distanceMeters:Int): Double = distanceMeters match {
      case v if v > 0 && v < 2800 => 4.0
      case v if v > 2900 && v < 3800 => 5.0
      case v if v > 3900 && v < 4800 => 6.0
      case v if v > 4900 && v < 5800 => 7.0
      case v if v > 5900 && v < 6800 => 8.0
      case v if v > 6900 && v < 7800 => 9.0
      case _ => 10.0
    }

    userManagerAPI.calculatePriceRoute(coordinateStart, coordinateFinish).flatMap {
      case dm:DistanceMatrix if dm.status == "OK" =>
        val result = for {
          row <- dm.rows
          element <- row.elements
        } yield {
          (calculateByDistance(element.distance.value), element.distance.value)
        }

        val price = result.map(_._1).headOption
        val distance = result.map(_._2).headOption
        Future.successful(VolskayaGetPriceResponse(price, distance, VolskayaSuccessResponse(responseMessage = getSuccessCalculateMessage(models.PriceFieldId))))
      case _ =>
        Future.failed(CalculatePriceRouteException(getFailedSendVerificationCode))
    }.recoverWith {
      case ex => Future.failed(ex)
    }
  }

  // orders section

  def getAllOrders(limit:Int, offset:Int): Future[Seq[OrderDomain]] =
    orderManagerAPI.getAllOrders(limit, offset).map(orders => orders.map(_.asDomain))

  def saveOrder(order: OrderDomain): Future[VolskayaResponse] = {
    orderManagerAPI.saveOrder(order).flatMap {
      case order: Order =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessSave(models.OrderFieldId)))
      case _ =>
        Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
    }.recoverWith {
      case ex =>
        Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage)))
    }
  }

}

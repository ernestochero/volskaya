package user

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import googleMapsService.{ContextFCM, ContextGoogleMaps, DistanceMatrixApi}
import googlefcmservice.SendNotificationApi
import models.UserManagementExceptions.{CalculatePriceRouteException, MatchPatternNotFoundException, SendVerificationCodeException, UserNotFoundException}
import models._
import akka.pattern.pipe
import googleMapsService.model.{DistanceMatrix, TravelMode, Units}
import googlefcmservice.model.SendNotification
import models.OrderManagementMessages.SaveOrder

import scala.concurrent.duration.Duration
import models.UserManagementMessages._
import models.VolskayaMessages._
import order.OrderStorageActor
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future

case class UserManagerAPI(system: ActorSystem) {

  def userManagementActor = system.actorSelection("/user/userManagementActor")
  val orderManagerAPI = OrderManagerAPI(system)

  import system.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  val log = system.log

  def getAllUsers(limit:Int, offset:Int): Future[Seq[UserDomain]] = {
    val response = (userManagementActor ? GetAllUsers(limit, offset)).mapTo[Seq[User]]
    response.map(users => users.map(_.asDomain))
  }

  def getUser(id: String): Future[VolskayaGetUserResponse] = {
    (userManagementActor ? GetUser(new ObjectId(id))).flatMap {
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
    (userManagementActor ? UpdatePassword(new ObjectId(id), oldPassword, newPassword)).flatMap {
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
    (userManagementActor ? VerifyLogin(email, password)).flatMap {
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
    (userManagementActor ? CheckCode(new ObjectId(id), code)).flatMap {
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
    (userManagementActor ? AddFavoriteSite(new ObjectId(id), favoriteSite)).flatMap {
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
    (userManagementActor ? SaveVerificationCode(id, verificationCode)).flatMap {
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
    (userManagementActor ? SendVerificationCode(verificationCode, phoneNumber)).flatMap {
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

  // TODO: improve this part
  def registerUser(email: String, password: String, phoneNumber: String): Future[VolskayaGetUserResponse] = {
    import scala.util.Random
    lazy val verificationCode = (1 to 6).foldLeft(""){(c,v) => s"$c${Random.nextInt(10)}"}
    lazy val msgVerificationCode = s"Tu codigo de verificacion es $verificationCode "
    val device = Device(name = "", number = phoneNumber, imei = "")
    val user = User(email = Some(email), password = Some(password), device = Some(device), favoriteSites = Some(List.empty[FavoriteSite]))
    val result = (userManagementActor ? SaveUser(user)).mapTo[User]
    result.flatMap { user =>
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

    (userManagementActor ? CalculatePriceRoute(coordinateStart, coordinateFinish)).flatMap {
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

  def saveOrder(order: OrderDomain) = orderManagerAPI.storeOrder(order.asResource)

}


class UserManager(userCollection: MongoCollection[User], googleMapsContext: ContextGoogleMaps, fcmContext: ContextFCM) extends Actor with ActorLogging {
  import context.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))
  val userStorage: ActorRef = context.watch(context.actorOf(Props(classOf[UserStorageActor], userCollection), "userStorage"))

  override def receive: Receive = {
    case msg@GetAllUsers(limit, offset) =>
      userStorage forward msg

    case msg@SaveUser(user) =>
      userStorage forward msg

    case msg@GetUser(id) =>
      userStorage forward msg

    case msg@UpdatePassword(id, oldPassword, newPassword) =>
      userStorage forward msg

    case msg@VerifyLogin(email, password) =>
      userStorage forward msg

    case msg@CheckCode(id, code) =>
      userStorage forward msg

    case msg@AddFavoriteSite(id, favoriteSite) =>
      userStorage forward msg

    case msg@SaveVerificationCode(id, verificationCode) =>
      userStorage forward msg

    case SendVerificationCode(verificationCode, phoneNumber) =>
      val result = SendNotificationApi.sendNotificationCode(verificationCode, phoneNumber)(fcmContext)
      result.pipeTo(sender())

    case CalculatePriceRoute(coordinateStart, coordinateFinish) =>
      val origins = coordinateStart.toString :: Nil
      val destinations = coordinateFinish.toString :: Nil
      val units = Some(Units.metric)
      val travelMode = Some(TravelMode.walking)
      val result = DistanceMatrixApi.getDistanceMatrix(origins, destinations, units, travelMode)(googleMapsContext)
      result.pipeTo(sender())
  }
}

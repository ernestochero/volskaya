package volskayaSystem

import akka.actor.ActorSystem
import akka.util.Timeout
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

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, SECONDS }

object PolygonUtils {

  val polygon = Array(
    Coordinate(-8.05168, -79.05373),
    Coordinate(-8.05203, -79.0549),
    Coordinate(-8.05276, -79.05525),
    Coordinate(-8.05478, -79.05453),
    Coordinate(-8.05644, -79.0539),
    Coordinate(-8.05757, -79.05377),
    Coordinate(-8.06004, -79.05603),
    Coordinate(-8.06538, -79.05813),
    Coordinate(-8.06873, -79.06107),
    Coordinate(-8.0716, -79.06391),
    Coordinate(-8.07354, -79.0643),
    Coordinate(-8.085, -79.05388),
    Coordinate(-8.08723, -79.05448),
    Coordinate(-8.08895, -79.05354),
    Coordinate(-8.09941, -79.05173),
    Coordinate(-8.10361, -79.04903),
    Coordinate(-8.10546, -79.04845),
    Coordinate(-8.11106, -79.05031),
    Coordinate(-8.11437, -79.05029),
    Coordinate(-8.11517, -79.0496),
    Coordinate(-8.12209, -79.0483),
    Coordinate(-8.12592, -79.05075),
    Coordinate(-8.13012, -79.05329),
    Coordinate(-8.13586, -79.05652),
    Coordinate(-8.136, -79.0586),
    Coordinate(-8.1352, -79.06034),
    Coordinate(-8.13454, -79.06228),
    Coordinate(-8.13692, -79.06537),
    Coordinate(-8.13839, -79.06395),
    Coordinate(-8.13977, -79.06237),
    Coordinate(-8.14259, -79.05907),
    Coordinate(-8.14537, -79.05577),
    Coordinate(-8.14718, -79.05358),
    Coordinate(-8.14898, -79.05148),
    Coordinate(-8.15024, -79.0499),
    Coordinate(-8.15161, -79.04801),
    Coordinate(-8.15368, -79.04558),
    Coordinate(-8.15404, -79.04261),
    Coordinate(-8.15351, -79.04164),
    Coordinate(-8.15261, -79.04159),
    Coordinate(-8.15126, -79.04048),
    Coordinate(-8.15268, -79.03893),
    Coordinate(-8.15334, -79.03695),
    Coordinate(-8.1527, -79.03554),
    Coordinate(-8.15075, -79.0335),
    Coordinate(-8.14578, -79.02905),
    Coordinate(-8.14352, -79.02718),
    Coordinate(-8.14253, -79.02462),
    Coordinate(-8.1403, -79.0239),
    Coordinate(-8.13865, -79.02316),
    Coordinate(-8.13698, -79.0229),
    Coordinate(-8.13649, -79.02139),
    Coordinate(-8.13701, -79.02061),
    Coordinate(-8.1385, -79.02028),
    Coordinate(-8.13995, -79.01944),
    Coordinate(-8.14111, -79.01796),
    Coordinate(-8.1403, -79.01526),
    Coordinate(-8.13842, -79.01309),
    Coordinate(-8.13666, -79.01422),
    Coordinate(-8.13471, -79.01587),
    Coordinate(-8.13404, -79.01829),
    Coordinate(-8.13253, -79.01997),
    Coordinate(-8.13222, -79.01992),
    Coordinate(-8.1318, -79.01983),
    Coordinate(-8.13072, -79.01955),
    Coordinate(-8.12791, -79.01745),
    Coordinate(-8.12672, -79.01668),
    Coordinate(-8.12562, -79.01575),
    Coordinate(-8.12513, -79.0128),
    Coordinate(-8.12396, -79.01011),
    Coordinate(-8.12302, -79.00579),
    Coordinate(-8.1223, -79.00286),
    Coordinate(-8.12047, -79.00079),
    Coordinate(-8.11638, -79.00264),
    Coordinate(-8.10676, -78.995),
    Coordinate(-8.10198, -78.99872),
    Coordinate(-8.09933, -78.99592),
    Coordinate(-8.09685, -78.99523),
    Coordinate(-8.09441, -78.99454),
    Coordinate(-8.09072, -78.99273),
    Coordinate(-8.08758, -78.99119),
    Coordinate(-8.08427, -78.99034),
    Coordinate(-8.07555, -78.99136),
    Coordinate(-8.07337, -78.99095),
    Coordinate(-8.07217, -78.99195),
    Coordinate(-8.07264, -78.99994),
    Coordinate(-8.07235, -79.00249),
    Coordinate(-8.07138, -79.00461),
    Coordinate(-8.07095, -79.0056),
    Coordinate(-8.07016, -79.00657),
    Coordinate(-8.0698, -79.00978),
    Coordinate(-8.06986, -79.01117),
    Coordinate(-8.07013, -79.01265),
    Coordinate(-8.07036, -79.01379),
    Coordinate(-8.07048, -79.01485),
    Coordinate(-8.07122, -79.01803),
    Coordinate(-8.07145, -79.01913),
    Coordinate(-8.07174, -79.02033),
    Coordinate(-8.07183, -79.02136),
    Coordinate(-8.07239, -79.02195),
    Coordinate(-8.0722, -79.02372),
    Coordinate(-8.07194, -79.02547),
    Coordinate(-8.07207, -79.03054),
    Coordinate(-8.07075, -79.03508),
    Coordinate(-8.06863, -79.03813),
    Coordinate(-8.06639, -79.04017),
    Coordinate(-8.06407, -79.04222),
    Coordinate(-8.06241, -79.04393),
    Coordinate(-8.05715, -79.04719),
    Coordinate(-8.05334, -79.05009),
    Coordinate(-8.05234, -79.05104),
    Coordinate(-8.05151, -79.05211)
  )

  def inside(point: Coordinate): Boolean = {
    // ray-casting algorithm based on
    // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    val (x, y) = point.getCoordinate

    var inside = false

    var i = 0
    var j = polygon.length - 1
    while ({
      i < polygon.length
    }) {
      val (xi, yi) = polygon(i).getCoordinate
      val (xj, yj) = polygon(j).getCoordinate

      val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
      if (intersect) inside = !inside

      j = {
        i += 1; i - 1
      }
    }

    inside
  }

}

case class VolskayaController(system: ActorSystem) {

  val userManagerAPI  = UserManagerAPI(system)
  val orderManagerAPI = OrderManagerAPI(system)

  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, SECONDS))

  val log = system.log

  def getAllUsers(limit: Int, offset: Int): Future[Seq[UserDomain]] =
    userManagerAPI.getAllUsers(limit: Int, offset: Int).map(users => users.map(_.asDomain))

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

  def calculatePriceRoute(coordinateStart: Coordinate,
                          coordinateFinish: Coordinate): Future[VolskayaGetPriceResponse] = {
    //TODO optimize this function when we have a geoZone
    def calculateByDistance(distanceMeters: Int): Double = distanceMeters match {
      case v if v > 0 && v < 2800    => 4.0
      case v if v > 2900 && v < 3800 => 5.0
      case v if v > 3900 && v < 4800 => 6.0
      case v if v > 4900 && v < 5800 => 7.0
      case v if v > 5900 && v < 6800 => 8.0
      case v if v > 6900 && v < 7800 => 9.0
      case _                         => 10.0
    }

    userManagerAPI
      .calculatePriceRoute(coordinateStart, coordinateFinish)
      .flatMap {
        case dm: DistanceMatrix if dm.status == "OK" =>
          val result = for {
            row     <- dm.rows
            element <- row.elements
          } yield {
            (calculateByDistance(element.distance.value), element.distance.value)
          }

          val price    = result.map(_._1).headOption
          val distance = result.map(_._2).headOption
          Future.successful(
            VolskayaGetPriceResponse(price,
                                     distance,
                                     VolskayaSuccessResponse(
                                       responseMessage =
                                         getSuccessCalculateMessage(models.PriceFieldId)
                                     ))
          )
        case _ =>
          Future.failed(CalculatePriceRouteException(getFailedSendVerificationCode))
      }
      .recoverWith {
        case ex => Future.failed(ex)
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

package user

import akka.pattern.ask
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.util.Timeout
import googleMapsService.{ DistanceMatrixApi, GoogleFCMContext, GoogleMapsContext }
import googlefcmservice.SendNotificationApi
import models._
import akka.pattern.pipe
import googleMapsService.model.{ DistanceMatrix, TravelMode, Units }
import googlefcmservice.model.SendNotification
import scala.concurrent.duration.{ Duration, SECONDS }
import models.UserManagementMessages._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future

case class UserManagerAPI(system: ActorSystem) {

  def userManagementActor = system.actorSelection("/user/userManagementActor")
  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, SECONDS))

  val log = system.log

  def getAllUsers(limit: Int, offset: Int): Future[Seq[User]] =
    (userManagementActor ? GetAllUsers(limit, offset)).mapTo[Seq[User]]

  def getUser(id: String): Future[Option[User]] =
    (userManagementActor ? GetUser(new ObjectId(id))).mapTo[Option[User]]

  def updatePassword(id: String, oldPassword: String, newPassword: String): Future[UpdateResult] =
    (userManagementActor ? UpdatePassword(new ObjectId(id), oldPassword, newPassword))
      .mapTo[UpdateResult]

  def verifyLogin(email: String, password: String): Future[Option[User]] =
    (userManagementActor ? VerifyLogin(email, password)).mapTo[Option[User]]

  def checkCode(id: String, code: String): Future[Boolean] =
    (userManagementActor ? CheckCode(new ObjectId(id), code)).mapTo[Boolean]

  def addFavoriteSite(id: String, favoriteSite: FavoriteSite): Future[UpdateResult] =
    (userManagementActor ? AddFavoriteSite(new ObjectId(id), favoriteSite)).mapTo[UpdateResult]

  def saveVerificationCode(id: ObjectId, verificationCode: String): Future[UpdateResult] =
    (userManagementActor ? SaveVerificationCode(id, verificationCode)).mapTo[UpdateResult]

  def sendVerificationCode(verificationCode: String,
                           phoneNumber: String): Future[SendNotification] =
    (userManagementActor ? SendVerificationCode(verificationCode, phoneNumber))
      .mapTo[SendNotification]

  def saveUser(user: User): Future[User] =
    (userManagementActor ? SaveUser(user)).mapTo[User]

  def calculateDistanceRoute(coordinateStart: Coordinate,
                             coordinateFinish: Coordinate): Future[DistanceMatrix] =
    (userManagementActor ? CalculatePriceRoute(coordinateStart, coordinateFinish))
      .mapTo[DistanceMatrix]

}

class UserManager(userCollection: MongoCollection[User],
                  googleMapsContext: GoogleMapsContext,
                  fcmContext: GoogleFCMContext)
    extends Actor
    with ActorLogging {
  import context.dispatcher

  implicit val timeout = Timeout(Duration.create(30, SECONDS))
  val userStorage: ActorRef =
    context.watch(context.actorOf(Props(classOf[UserStorageActor], userCollection), "userStorage"))

  override def receive: Receive = {
    case msg @ GetAllUsers(limit, offset) =>
      userStorage forward msg

    case msg @ SaveUser(user) =>
      userStorage forward msg

    case msg @ GetUser(id) =>
      userStorage forward msg

    case msg @ UpdatePassword(id, oldPassword, newPassword) =>
      userStorage forward msg

    case msg @ VerifyLogin(email, password) =>
      userStorage forward msg

    case msg @ CheckCode(id, code) =>
      userStorage forward msg

    case msg @ AddFavoriteSite(id, favoriteSite) =>
      userStorage forward msg

    case msg @ SaveVerificationCode(id, verificationCode) =>
      userStorage forward msg

    case SendVerificationCode(verificationCode, phoneNumber) =>
      val result =
        SendNotificationApi.sendNotificationCode(verificationCode, phoneNumber)(fcmContext)
      result.pipeTo(sender())

    case CalculatePriceRoute(coordinateStart, coordinateFinish) =>
      val origins      = coordinateStart.toString :: Nil
      val destinations = coordinateFinish.toString :: Nil
      val units        = Some(Units.metric)
      val travelMode   = Some(TravelMode.walking)
      val result = DistanceMatrixApi.getDistanceMatrix(origins, destinations, units, travelMode)(
        googleMapsContext
      )
      result.pipeTo(sender())
  }
}

package user

import java.util.concurrent.TimeUnit

import akka.actor.Status.Failure
import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import googleMapsService.{ContextFCM, ContextGoogleMaps}
import models.UserManagementExceptions.UserNotFoundException
import models.{User, UserDomain}

import scala.concurrent.duration.Duration
import models.UserManagementMessages._
import models.VolskayaMessages.{VolskayaGetUserResponse, VolskayaSuccessResponse, getSuccessGetMessage}
import mongodb.Mongo
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.{Await, Future}

case class UserManagerAPI(system: ActorSystem) {

  def userManagementActor = system.actorSelection("/user/userManagementActor")

  import system.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  def getAllUsers(limit:Int, offset:Int): Future[Seq[UserDomain]] = {
    val response = (userManagementActor ? GetAllUsers(limit, offset)).mapTo[Seq[User]]
    response.map(users => users.map(_.asDomain))
  }

  def getUser(id: String): Future[VolskayaGetUserResponse] = {
    val response = (userManagementActor ? GetUser(new ObjectId(id))).flatMap {
      case user:User =>
        Future.successful(VolskayaGetUserResponse(Some(user.asDomain), VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField))))
    }.recoverWith {
      case ex => Future.failed(UserNotFoundException(ex.getMessage))
    }
    response
  }

}


class UserManager(collection: MongoCollection[User], googleMapsContext: ContextGoogleMaps, fcmContext: ContextFCM) extends Actor with ActorLogging {
  import context.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))
  val userStorage: ActorRef = context.watch(context.actorOf(Props(classOf[UserStorageActor], collection), "userStorage"))

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
  }
}

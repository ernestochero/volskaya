package user

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import googleMapsService.{ContextFCM, ContextGoogleMaps}
import models.User

import scala.concurrent.duration.Duration
import models.UserManagementMessages._
import org.mongodb.scala.MongoCollection

case class UserManagerAPI(system: ActorSystem) {

  def userManagementActor = system.actorSelection("/user/userManagementActor")


  import system.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

}


class UserManager(collection: MongoCollection[User], googleMapsContext: ContextGoogleMaps, fcmContext: ContextFCM) extends Actor with ActorLogging{
  import context.dispatcher

  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  val userStorage: ActorRef = context.watch(context.actorOf(Props(classOf[UserStorageActor], collection), "userStorage"))

  override def receive: Receive = {
    case msg@GetAllUsers =>
      userStorage forward msg

    case msg@SaveUser =>
      userStorage forward msg

    case msg@GetUser =>
      userStorage forward msg

    case msg@UpdatePassword =>
      userStorage forward msg

    case msg@VerifyLogin =>
      userStorage forward msg

    case msg@CheckCode =>
      userStorage forward msg

    case msg@SaveUser =>
      userStorage forward msg

    case msg@AddFavoriteSite =>
      userStorage forward msg
  }
}

package user

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.util.Timeout
import models.User
import models.UserManagementMessages._
import org.mongodb.scala.MongoCollection

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

case class UserStorageActorAPI(system: ActorSystem) {
  def userStorageActor = system.actorSelection("/user/userStorageActor")
  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

}

class UserStorageActor(collection: MongoCollection[User]) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  override def receive: Receive = {
    case SaveUser(user) =>

  }

}

package user

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.util.Timeout
import models.User
import models.UserManagementMessages._
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import akka.pattern.pipe

case class UserStorageActorAPI(system: ActorSystem) {
  def userStorageActor = system.actorSelection("/user/userStorageActor")
  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

}

class UserStorageActor(collection: MongoCollection[User]) extends Actor with ActorLogging  {
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  override def receive: Receive = {
    case SaveUser(user) =>
      val result =  collection.insertOne(user).head().map {_ => user }
      sender() ! result

    case GetAllUsers(limit, offset) =>
      val result = collection.find().skip(offset).limit(limit).toFuture()
      result.pipeTo(sender())

    case GetUser(id) =>
      val filter = Document("_id" -> id)
      val result = collection.find(filter).first().head()
      sender() ! result

    case VerifyLogin(email, password) =>
      val filter = Document("email" -> email, "password" -> password)
      val result = collection.find(filter).first().head()
      sender() ! result

    case UpdateEmail(id, email) =>
      val filter = Document("_id" -> id)
      val update = Document("$set" ->  Document("email" -> email))
      val result = collection.findOneAndUpdate(filter, update).head()
      sender() ! result

    case UpdatePassword(id, oldPassword, newPassword) =>
      val filter = Document("_id" -> id, "password" -> oldPassword)
      val update = Document("$set" -> Document("password" -> newPassword))
      val result = collection.updateOne(filter, update).head()
      sender() ! result

    case UpdatePersonalInformation(id, personalInformation) =>
      val filter = Document("_id" -> id)
      val fields = Document(
        "firstName" -> personalInformation.firstName,
        "lastName" -> personalInformation.lastName,
        "dni" -> personalInformation.dni
      )
      val update = Document("$set" -> Document("personalInformation" -> fields))
      val result = collection.findOneAndUpdate(filter, update).head()
      sender() ! result

    case AddFavoriteSite(id, favoriteSite) =>
      val favoriteSiteField = Document(
        "coordinate" -> Document("latitude" -> favoriteSite.coordinate.latitude, "longitude" -> favoriteSite.coordinate.longitude),
        "name" -> favoriteSite.name,
        "address" -> favoriteSite.address
      )
      val filter = Document("_id" -> id)
      val update = Document("$push" -> Document("favoriteSites" -> favoriteSiteField))
      val result = collection.updateOne(filter, update).head()
      sender() ! result

    case SaveConfirmationCode(id, confirmationCode) =>
      val filter = Document("_id" -> id)
      val update = Document("$set" -> Document("confirmationCode" -> confirmationCode))
      val result = collection.updateOne(filter, update).head()
      sender() ! result

    case CheckCode(id, code) =>
      val filter = Document("_id" -> id, "confirmationCode" -> code)
      sender() ! collection.find(filter).head()
  }

}

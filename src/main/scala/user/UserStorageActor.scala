package user

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.util.Timeout
import models.User
import models.UserManagementMessages._
import org.mongodb.scala.{Document, MongoCollection}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import akka.pattern.pipe

class UserStorageActor(collection: MongoCollection[User]) extends Actor with ActorLogging  {
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  override def receive: Receive = {
    case SaveUser(user) =>
      val result =  collection.insertOne(user)
          .toFuture()
          .recoverWith { case e => Future.failed(e) }
          .map(_ => user)
      result.pipeTo(sender())

    case GetAllUsers(limit, offset) =>
      val result = collection.find().skip(offset).limit(limit).toFuture()
      result.pipeTo(sender())

    case GetUser(id) =>
      val filter = Document("_id" -> id)
      val result = collection.find(filter)
          .toFuture()
          .recoverWith {  case e => Future.failed(e) }
          .map(_.headOption)
      result.pipeTo(sender())

    case VerifyLogin(email, password) =>
      val filter = Document("email" -> email, "password" -> password)
      val result = collection.find(filter)
          .toFuture()
          .recoverWith{ case e => Future.failed(e) }
          .map(_.headOption)
      result.pipeTo(sender())

    case UpdateEmail(id, email) =>
      val filter = Document("_id" -> id)
      val update = Document("$set" ->  Document("email" -> email))
      val result = collection.findOneAndUpdate(filter, update).head()
      result.pipeTo(sender())

    case UpdatePassword(id, oldPassword, newPassword) =>
      val filter = Document("_id" -> id, "password" -> oldPassword)
      val update = Document("$set" -> Document("password" -> newPassword))
      val result = collection.updateOne(filter, update)
          .toFuture()
          .recoverWith { case e => Future.failed(e) }
      result.pipeTo(sender())

    case UpdatePersonalInformation(id, personalInformation) =>
      val filter = Document("_id" -> id)
      val fields = Document(
        "firstName" -> personalInformation.firstName,
        "lastName" -> personalInformation.lastName,
        "dni" -> personalInformation.dni
      )
      val update = Document("$set" -> Document("personalInformation" -> fields))
      val result = collection.findOneAndUpdate(filter, update)
          .toFuture()
          .recoverWith { case e => Future.failed(e) }
      result.pipeTo(sender())

    case AddFavoriteSite(id, favoriteSite) =>
      val favoriteSiteField = Document(
        "coordinate" -> Document("latitude" -> favoriteSite.coordinate.latitude, "longitude" -> favoriteSite.coordinate.longitude),
        "name" -> favoriteSite.name,
        "address" -> favoriteSite.address
      )
      val filter = Document("_id" -> id)
      val update = Document("$push" -> Document("favoriteSites" -> favoriteSiteField))
      val result = collection.updateOne(filter, update)
          .toFuture()
          .recoverWith { case e => Future.failed(e) }
      result.pipeTo(sender())

    case SaveVerificationCode(id, verificationCode) =>
      val filter = Document("_id" -> id)
      val update = Document("$set" -> Document("confirmationCode" -> verificationCode))
      val result = collection.updateOne(filter, update)
          .toFuture()
          .recoverWith { case e => Future.failed(e) }
      result.pipeTo(sender())

    case CheckCode(id, code) =>
      val filter = Document("_id" -> id, "confirmationCode" -> code)
      val update = Document("$set" -> Document("isAuthenticated" -> true))
      val result = collection.findOneAndUpdate(filter, update)
          .toFuture()
          .recoverWith{ case e => Future.failed(e) }
          .map {
            case user: User => true
            case _ => false
          }
      result.pipeTo(sender())
  }

}

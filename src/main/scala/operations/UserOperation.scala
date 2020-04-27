package operations

import models.{ FavoriteSite, PersonalInformation, User }
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{ Document, ObjectId }
import org.mongodb.scala.result.UpdateResult
import zio.{ Task, UIO, ZIO }
import commons.Transformers._

import scala.concurrent.{ ExecutionContext, Future }
object UserOperation {
  implicit val ec: ExecutionContext = ExecutionContext.global
  def getUserFromDatabase(userMongoCollection: MongoCollection[User],
                          id: String): Task[Option[User]] = {
    val filter = Document("_id" -> new ObjectId(id))
    userMongoCollection
      .find(filter)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .map(_.headOption)
      .toTask
  }
  def getAllUsersFromDatabase(limit: Int,
                              offset: Int,
                              userMongoCollection: MongoCollection[User]): Task[List[User]] =
    userMongoCollection.find().skip(offset).limit(limit).toFuture().map(_.toList).toTask

  def updatePasswordDatabase(id: String,
                             oldPassword: String,
                             newPassword: String,
                             userMongoCollection: MongoCollection[User]): Task[UpdateResult] = {
    val filter = Document("_id"  -> new ObjectId(id), "password" -> oldPassword)
    val update = Document("$set" -> Document("password" -> newPassword))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toRIO
  }
  def insertUserDatabase(user: User,
                         userMongoCollection: MongoCollection[User]): Task[Option[User]] =
    userMongoCollection
      .insertOne(user)
      .toFuture()
      .recoverWith { case ex => Future.failed(ex) }
      .map(_ => Some(user))
      .toTask

  def updateFavoriteSiteDatabase(
    id: String,
    favoriteSite: FavoriteSite,
    userMongoCollection: MongoCollection[User]
  ): Task[UpdateResult] = {
    val filter = Document("_id" -> new ObjectId(id))
    val favoriteSiteField = Document(
      "coordinate" -> Document("latitude" -> favoriteSite.coordinate.latitude,
                               "longitude" -> favoriteSite.coordinate.longitude),
      "name"    -> favoriteSite.name,
      "address" -> favoriteSite.address
    )
    val update = Document("$push" -> Document("favoriteSites" -> favoriteSiteField))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toTask
  }

  def updatePersonalInformationDatabase(
    id: String,
    personalInformation: PersonalInformation,
    userMongoCollection: MongoCollection[User]
  ): Task[UpdateResult] = {
    val filter = Document("_id" -> new ObjectId(id))
    val fields = Document(
      "firstName" -> personalInformation.firstName,
      "lastName"  -> personalInformation.lastName,
      "dni"       -> personalInformation.dni
    )
    val update = Document("$set" -> Document("personalInformation" -> fields))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toTask
  }

  def updateEmailDatabase(id: String,
                          email: String,
                          userMongoCollection: MongoCollection[User]): Task[UpdateResult] = {
    val filter = Document("_id"  -> new ObjectId(id))
    val update = Document("$set" -> Document("email" -> email))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toTask
  }
}

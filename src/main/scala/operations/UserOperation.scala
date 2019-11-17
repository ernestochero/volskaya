package operations

import models.{ FavoriteSite, PersonalInformation, User }
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{ Document, ObjectId }
import org.mongodb.scala.result.UpdateResult
import zio.RIO
import zio.console.Console
import commons.Transformers._
import scala.concurrent.{ ExecutionContext, Future }

sealed trait UserCollectionOperation extends Operation {
  def getAllUsersFromDatabase(limit: Int, offset: Int): RIO[Console, List[User]]
  def wakeUpHeroku: RIO[Console, String] = RIO.succeed("I'm awake")
  def getUserFromDatabase(id: String): RIO[Console, Option[User]]
  def updatePasswordDatabase(id: String,
                             oldPassword: String,
                             newPassword: String): RIO[Console, UpdateResult]
  def updateEmailDatabase(id: String, email: String): RIO[Console, UpdateResult]
  def updatePersonalInformationDatabase(
    id: String,
    personalInformation: PersonalInformation
  ): RIO[Console, UpdateResult]

  def updateFavoriteSiteDatabase(id: String, favoriteSite: FavoriteSite): RIO[Console, UpdateResult]
  def insertUserDatabase(user: User): RIO[Console, Option[User]]

  /*  def verifyLoginAgainstDatabase(email: String, password: String)
  def storeVerificationCodeDatabase(id: String, verificationCode: String)
  def checkVerificationCodeDatabase(id: String, verificationCode: String)*/
}

final case class UserOperation(userMongoCollection: MongoCollection[User])
    extends UserCollectionOperation {
  implicit val ec: ExecutionContext = ExecutionContext.global
  override def getAllUsersFromDatabase(limit: Int, offset: Int): RIO[Console, List[User]] =
    userMongoCollection.find().skip(offset).limit(limit).toFuture().map(_.toList).toRIO
  override def getUserFromDatabase(id: String): RIO[Console, Option[User]] = {
    val filter = Document("_id" -> new ObjectId(id))
    userMongoCollection
      .find(filter)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .map(_.headOption)
      .toRIO
  }

  override def updatePasswordDatabase(id: String,
                                      oldPassword: String,
                                      newPassword: String): RIO[Console, UpdateResult] = {
    val filter = Document("_id"  -> new ObjectId(id), "password" -> oldPassword)
    val update = Document("$set" -> Document("password" -> newPassword))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toRIO
  }

  override def updateEmailDatabase(id: String, email: String): zio.RIO[Console, UpdateResult] = {
    val filter = Document("_id"  -> new ObjectId(id))
    val update = Document("$set" -> Document("email" -> email))
    userMongoCollection
      .updateOne(filter, update)
      .toFuture()
      .recoverWith { case e => Future.failed(e) }
      .toRIO
  }

  override def updatePersonalInformationDatabase(
    id: String,
    personalInformation: PersonalInformation
  ): zio.RIO[Console, UpdateResult] = {
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
      .toRIO
  }

  override def updateFavoriteSiteDatabase(
    id: String,
    favoriteSite: FavoriteSite
  ): zio.RIO[Console, UpdateResult] = {
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
      .toRIO
  }

  override def insertUserDatabase(user: User): RIO[Console, Option[User]] =
    userMongoCollection
      .insertOne(user)
      .toFuture()
      .recoverWith { case ex => Future.failed(ex) }
      .map(_ => Some(user))
      .toRIO
}

package modules

import commons.Transformers._
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages._
import models.{ FavoriteSite, PasswordField, PersonalInformation, User }
import modules.UserCollectionModule._
import mongodb.Mongo
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{ Document, ObjectId }
import org.mongodb.scala.result.UpdateResult
import zio.console.Console
import zio.{ RIO, ZIO }

import scala.concurrent.{ ExecutionContext, Future }

sealed trait Operation {
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

  /*  def verifyLoginAgainstDatabase(email: String, password: String)
  def storeVerificationCodeDatabase(id: String, verificationCode: String)
  def checkVerificationCodeDatabase(id: String, verificationCode: String)*/
}

trait UserCollectionModule {
  val userCollectionModule: Service[Any]
}
object UserCollectionModule {

  final case class UserOperation(userMongoCollection: MongoCollection[User]) extends Operation {
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

  }

  case class UserCollection(userMongoCollection: MongoCollection[User]) {
    val userOperation = UserOperation(userMongoCollection)
    def getUser(
      id: String
    ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option, User]] =
      for {
        user <- userOperation
          .getUserFromDatabase(id)
          .mapError(
            e => VolskayaAPIException(e.getMessage)
          )
        volskayaResult = VolskayaResultSuccessResponse[Option, User](
          value = user,
          if (user.isDefined)
            VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField))
          else
            VolskayaFailedResponse(
              responseMessage = getUserNotExistMessage
            )
        )
      } yield volskayaResult

    def getAllUsers(
      limit: Int,
      offset: Int
    ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[List, User]] =
      for {
        users <- userOperation
          .getAllUsersFromDatabase(limit, offset)
          .mapError(e => VolskayaAPIException(e.getMessage))
        volskayaResult = VolskayaResultSuccessResponse[List, User](
          users,
          VolskayaSuccessResponse(responseMessage = "Users Extracted successfully")
        )
      } yield volskayaResult

    def updatePassword(
      id: String,
      oldPassword: String,
      newPassword: String
    ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option, String]] =
      for {
        updateResult <- userOperation
          .updatePasswordDatabase(
            id,
            oldPassword,
            newPassword
          )
          .mapError(e => VolskayaAPIException(e.getMessage))
        volskayaResult = VolskayaResultSuccessResponse[Option, String](
          Some(id),
          if (updateResult.getMatchedCount == 1 && updateResult.wasAcknowledged()) {
            VolskayaSuccessResponse(
              responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)
            )
          } else {
            VolskayaFailedResponse(
              responseMessage = getUserNotExistMessage
            )
          }
        )
      } yield volskayaResult
  }

  trait Service[R] {
    def userCollection(
      uri: String,
      databaseName: String,
      userCollectionName: String
    ): ZIO[R, Throwable, UserCollection]
  }

  trait Live extends UserCollectionModule {
    override val userCollectionModule: Service[Any] = new Service[Any] {
      override def userCollection(
        uri: String,
        databaseName: String,
        userCollectionName: String
      ): ZIO[Any, Throwable, UserCollection] =
        for {
          mongoCollection <- Mongo
            .setupMongoConfiguration[User](uri, databaseName, userCollectionName)
        } yield UserCollection(mongoCollection)
    }
  }

  object factory extends Service[UserCollectionModule] {
    override def userCollection(
      uri: String,
      databaseName: String,
      userCollectionName: String
    ): ZIO[UserCollectionModule, Throwable, UserCollection] =
      ZIO.accessM[UserCollectionModule](
        _.userCollectionModule.userCollection(
          uri,
          databaseName,
          userCollectionName
        )
      )
  }
}
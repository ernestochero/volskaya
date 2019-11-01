package graphql
import zio.{ RIO, ZIO }
import zio.console.Console
import UserCollectionModule._
import models.{ FavoriteSite, PersonalInformation, User }
import org.mongodb.scala.MongoCollection
import commons.Transformers._
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.{
  VolskayaFailedResponse,
  VolskayaResultSuccessResponse,
  VolskayaSuccessResponse,
  getSuccessGetMessage,
  getUserNotExistMessage
}
import mongodb.Mongo
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document

import scala.concurrent.{ ExecutionContext, Future }

sealed trait Operation {
  def getAllUsersFromDatabase(limit: Int, offset: Int): RIO[Console, List[User]]
  def wakeUpHeroku: RIO[Console, String] = RIO.succeed("I'm awake")
  def getUserFromDatabase(id: String): RIO[Console, Option[User]]
  /*  def verifyLoginAgainstDatabase(email: String, password: String)
  def updateEmailDatabase(id: String, email: String)
  def updatePasswordDatabase(id: String, oldPassword: String, newPassword: String)
  def updatePersonalInformationDatabase(id: String, personalInformation: PersonalInformation)
  def addFavoriteSiteDatabase(id: String, favoriteSite: FavoriteSite)
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
  }

  //case class UserCollection(mongoCollection: MongoCollection[User])

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

package modules

import caliban.CalibanError.ExecutionError
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages._
import models.{ PasswordField, User }
import modules.UserCollectionModule._
import mongodb.Mongo
import org.mongodb.scala.MongoCollection
import zio.console.Console
import zio.ZIO
trait UserCollectionModule {
  val userCollectionModule: Service[Any]
}
object UserCollectionModule {
  case class UserCollection(userMongoCollection: MongoCollection[User]) {
    val userOperation = operations.UserOperation(userMongoCollection)
    def getUser(
      id: String
    ): ZIO[Console, VolskayaAPIException, VolskayaResult[Option, User]] =
      for {
        user <- userOperation
          .getUserFromDatabase(id)
          .mapError(
            e => VolskayaAPIException(e.getMessage)
          )
        volskayaResult = VolskayaResult[Option, User](
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
    ): ZIO[Console, VolskayaAPIException, VolskayaResult[List, User]] =
      for {
        users <- userOperation
          .getAllUsersFromDatabase(limit, offset)
          .mapError(e => VolskayaAPIException(e.getMessage))
        volskayaResult = VolskayaResult[List, User](
          users,
          VolskayaSuccessResponse(responseMessage = "Users Extracted successfully")
        )
      } yield volskayaResult

    def updatePassword(
      id: String,
      oldPassword: String,
      newPassword: String
    ): ZIO[Console, VolskayaAPIException, VolskayaResult[Option, String]] =
      for {
        updateResult <- userOperation
          .updatePasswordDatabase(
            id,
            oldPassword,
            newPassword
          )
          .mapError(e => VolskayaAPIException(e.getMessage))
        volskayaResult = VolskayaResult[Option, String](
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

    def insertUser(
      user: User
    ): ZIO[Console, ExecutionError, VolskayaResult[Option, User]] =
      for {
        user <- userOperation
          .insertUserDatabase(user)
          .mapError(ex => ExecutionError(s"Insert Operation Error : ${ex.getMessage}"))
        volskayaResult = VolskayaResult(
          user,
          VolskayaSuccessResponse(
            responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)
          )
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
    override val userCollectionModule: Service[Any] =
      (uri: String, databaseName: String, userCollectionName: String) =>
        for {
          mongoCollection <- Mongo
            .setupMongoConfiguration[User](uri, databaseName, userCollectionName)
        } yield UserCollection(mongoCollection)
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

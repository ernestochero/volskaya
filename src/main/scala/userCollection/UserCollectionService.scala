package userCollection

import caliban.CalibanError.ExecutionError
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages._
import models.{ PasswordField, User }
import operations.UserOperation
import org.mongodb.scala.MongoCollection
import zio.{ Has, Queue, Ref, UIO, ZIO, ZLayer }
object UserCollectionService {
  type UserCollectionServiceType = Has[Service]
  trait Service {
    def getUser(
      id: String
    ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, User]]
    def getAllUsers(
      limit: Int,
      offset: Int
    ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[List, User]]
    def updatePassword(
      id: String,
      oldPassword: String,
      newPassword: String
    ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, String]]
    def insertUser(
      user: User
    ): ZIO[UserCollectionServiceType, ExecutionError, VolskayaResult[Option, User]]
  }

  def wakeUpHeroku: UIO[String] = ZIO.succeed("I'm awake")

  def getUser(
    id: String
  ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, User]] =
    ZIO.accessM[UserCollectionServiceType](_.get.getUser(id))

  def getAllUsers(
    limit: Int,
    offset: Int
  ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[List, User]] =
    ZIO.accessM[UserCollectionServiceType](_.get.getAllUsers(limit, offset))

  def updatePassword(
    id: String,
    oldPassword: String,
    newPassword: String
  ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, String]] =
    ZIO.accessM[UserCollectionServiceType](_.get.updatePassword(id, oldPassword, newPassword))

  def insertUser(
    user: User
  ): ZIO[UserCollectionServiceType, ExecutionError, VolskayaResult[Option, User]] =
    ZIO.accessM[UserCollectionServiceType](_.get.insertUser(user))

  def make(userCollection: MongoCollection[User]): ZLayer[Any, Nothing, UserCollectionServiceType] =
    ZLayer.fromEffect {
      for {
        userCollectionRef <- Ref.make(userCollection)
        subscribers       <- Ref.make(List.empty[Queue[String]])
      } yield
        new Service {
          override def getUser(
            id: String
          ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, User]] =
            for {
              userCollection <- userCollectionRef.get
              user <- UserOperation
                .getUserFromDatabase(userCollection, id)
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

          override def getAllUsers(
            limit: Int,
            offset: Int
          ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[List, User]] =
            for {
              userCollection <- userCollectionRef.get
              users <- UserOperation
                .getAllUsersFromDatabase(limit, offset, userCollection)
                .mapError(e => VolskayaAPIException(e.getMessage))
              volskayaResult = VolskayaResult[List, User](
                users,
                VolskayaSuccessResponse(responseMessage = "Users Extracted successfully")
              )
            } yield volskayaResult

          override def updatePassword(
            id: String,
            oldPassword: String,
            newPassword: String
          ): ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option, String]] =
            for {
              userCollection <- userCollectionRef.get
              updateResult <- UserOperation
                .updatePasswordDatabase(
                  id,
                  oldPassword,
                  newPassword,
                  userCollection
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

          override def insertUser(
            user: User
          ): ZIO[UserCollectionServiceType, ExecutionError, VolskayaResult[Option, User]] =
            for {
              userCollection <- userCollectionRef.get
              user <- UserOperation
                .insertUserDatabase(user, userCollection)
                .mapError(ex => ExecutionError(s"Insert Operation Error : ${ex.getMessage}"))
              volskayaResult = VolskayaResult(
                user,
                VolskayaSuccessResponse(
                  responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)
                )
              )
            } yield volskayaResult
        }
    }
}

package graphql

import graphql.UserCollectionModule.UserCollection
import models.User
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.VolskayaResultSuccessResponse
import zio.{ Queue, Ref, UIO, ZIO }
import zio.console.Console
import zio.stream.ZStream
case class VolskayaService(userCollection: Ref[UserCollection],
                           subscribers: Ref[List[Queue[String]]]) {

  def getAllUsers(
    limit: Int,
    offset: Int
  ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[List, User]] =
    userCollection.get.flatMap(
      _.getAllUsers(limit, offset)
    )

  def getUser(
    id: String
  ): ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option, User]] =
    userCollection.get.flatMap(
      _.getUser(id)
    )

  def userAddedEvent: ZStream[Any, Nothing, String] = ZStream.unwrap {
    for {
      queue <- Queue.unbounded[String]
      _     <- subscribers.update(queue :: _)
    } yield ZStream.fromQueue(queue)
  }
}

object VolskayaService {
  def make(userCollection: UserCollection): UIO[VolskayaService] =
    for {
      state       <- Ref.make(userCollection)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new VolskayaService(state, subscribers)
}

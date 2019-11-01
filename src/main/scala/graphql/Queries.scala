package graphql

import caliban.schema.Annotations.GQLDescription
import models.User
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.VolskayaResultSuccessResponse
import zio.{ UIO, ZIO }
import zio.console.Console

case class idArg(id: String)
case class limitOffsetArg(limit: Int, offset: Int)

case class Queries(
  @GQLDescription("Volskaya return a user by id")
  getUserById: idArg => ZIO[Console, VolskayaAPIException, VolskayaResultSuccessResponse[Option,
                                                                                         User]],
  @GQLDescription("Volskaya return a list of users")
  getAllUsers: limitOffsetArg => ZIO[Console,
                                     VolskayaAPIException,
                                     VolskayaResultSuccessResponse[List, User]],
  @GQLDescription("Volskaya return a list of users")
  wakeUpHeroku: UIO[String]
)

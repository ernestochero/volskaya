package graphql

import caliban.CalibanError.ExecutionError
import caliban.schema.Annotations.GQLDescription
import models.{ Coordinate, User }
import models.VolskayaMessages.{ VolskayaPrice, VolskayaResultSuccessResponse }
import zio.{ UIO, ZIO }
import zio.console.Console

case class idArg(id: String)
case class limitOffsetArg(limit: Int, offset: Int)
case class calculatePriceRouteArg(coordinateStart: Coordinate, coordinateFinish: Coordinate)
case class Queries(
  @GQLDescription("Volskaya return a user by id")
  getUserById: idArg => ZIO[Console, ExecutionError, VolskayaResultSuccessResponse[Option, User]],
  @GQLDescription("Volskaya return a list of users")
  getAllUsers: limitOffsetArg => ZIO[Console, ExecutionError, VolskayaResultSuccessResponse[List,
                                                                                            User]],
  @GQLDescription("Volskaya return a list of users")
  wakeUpHeroku: UIO[String],
  @GQLDescription("Volskaya return price of one Route")
  calculatePriceRoute: calculatePriceRouteArg => ZIO[
    Console,
    ExecutionError,
    VolskayaResultSuccessResponse[Option, VolskayaPrice]
  ]
)

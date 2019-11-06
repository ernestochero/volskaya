package graphql

import caliban.CalibanError.ExecutionError
import caliban.schema.Annotations.GQLDescription
import models.{ Coordinate, Role, User }
import models.VolskayaMessages.{ VolskayaPrice, VolskayaResult }
import zio.{ UIO, ZIO }
import zio.console.Console

case class idArg(id: String)
case class limitOffsetArg(limit: Option[Int], offset: Option[Int])
case class calculatePriceRouteArg(coordinateStart: Coordinate, coordinateFinish: Coordinate)
case class insertRole(role: Role)
case class Queries(
  @GQLDescription("Volskaya return a user by id")
  getUserById: idArg => ZIO[Console, ExecutionError, VolskayaResult[Option, User]],
  @GQLDescription("Volskaya return a list of users")
  getAllUsers: limitOffsetArg => ZIO[Console, ExecutionError, VolskayaResult[List, User]],
  @GQLDescription("Volskaya return a list of users")
  wakeUpHeroku: UIO[String],
  @GQLDescription("Volskaya return price of one Route")
  calculatePriceRoute: calculatePriceRouteArg => ZIO[
    Console,
    ExecutionError,
    VolskayaResult[Option, VolskayaPrice]
  ]
)

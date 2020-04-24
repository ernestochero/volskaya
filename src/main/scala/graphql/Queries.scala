package graphql
import caliban.schema.Annotations.GQLDescription
import models.UserManagementExceptions.VolskayaAPIException
import models.{ Coordinate, Role, User }
import models.VolskayaMessages.{ VolskayaPrice, VolskayaResult }
import userCollection.UserCollectionService.UserCollectionServiceType
import googleMaps.googleMapsService.GoogleMapsServiceType
import zio.{ UIO, ZIO }

case class idArg(id: String)
case class limitOffsetArg(limit: Option[Int], offset: Option[Int])
case class calculatePriceRouteArg(coordinateStart: Coordinate, coordinateFinish: Coordinate)
case class insertRole(role: Role)
case class Queries(
  @GQLDescription("Volskaya return a user by id")
  getUserById: idArg => ZIO[UserCollectionServiceType, VolskayaAPIException, VolskayaResult[Option,
                                                                                            User]],
  @GQLDescription("Volskaya return a list of users")
  getAllUsers: limitOffsetArg => ZIO[UserCollectionServiceType,
                                     VolskayaAPIException,
                                     VolskayaResult[List, User]],
  @GQLDescription("Volskaya return a list of users")
  wakeUpHeroku: UIO[String],
  @GQLDescription("Volskaya return price of one Route")
  calculatePriceRoute: calculatePriceRouteArg => ZIO[
    GoogleMapsServiceType,
    VolskayaAPIException,
    VolskayaPrice
  ]
)

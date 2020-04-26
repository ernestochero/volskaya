package graphql
import caliban.schema.Annotations.GQLDescription
import models.UserManagementExceptions.VolskayaAPIException
import models.{ Coordinate, Role }
import models.VolskayaMessages.{ VolskayaPrice }
import googleMaps.googleMapsService.GoogleMapsServiceType
import zio.{ UIO, ZIO }

case class idArg(id: String)
case class limitOffsetArg(limit: Option[Int], offset: Option[Int])
case class calculatePriceRouteArg(coordinateStart: Coordinate, coordinateFinish: Coordinate)
case class insertRole(role: Role)
case class Queries(
  @GQLDescription("Volskaya return a message of wakeup")
  wakeUpHeroku: UIO[String],
  @GQLDescription("Volskaya return price of one Route")
  calculatePriceRoute: calculatePriceRouteArg => ZIO[
    GoogleMapsServiceType,
    VolskayaAPIException,
    VolskayaPrice
  ]
)

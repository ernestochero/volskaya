package modules
import commons.Transformers._
import modules.GoogleMapsModule._
import googleMapsService.model.{ DistanceMatrix, TravelMode, Units }
import googleMapsService.{ DistanceMatrixApi, GoogleMapsContext }
import models.Coordinate
import zio.{ RIO, ZIO }
import zio.console.Console

trait GoogleMapsModule {
  val googleMapsModule: Service[Any]
}

object GoogleMapsModule {
  case class GoogleMapsService(googleMapsContext: GoogleMapsContext) {
    implicit val context: GoogleMapsContext = googleMapsContext
    def calculateDistanceMatrixFromCoordinates(
      coordinateStart: Coordinate,
      coordinateFinish: Coordinate
    ): RIO[Console, DistanceMatrix] =
      DistanceMatrixApi
        .getDistanceMatrix(
          origins = coordinateStart.toString :: Nil,
          destinations = coordinateFinish.toString :: Nil,
          units = Some(Units.metric),
          mode = Some(TravelMode.walking)
        )
        .toRIO
  }
  trait Service[R] {
    def googleMapsService(
      googleMapsContext: GoogleMapsContext
    ): ZIO[R, Throwable, GoogleMapsService]
  }
  trait Live extends GoogleMapsModule {
    override val googleMapsModule: Service[Any] = (googleMapsContext: GoogleMapsContext) =>
      ZIO.succeed(GoogleMapsService(googleMapsContext))
  }

  object factory extends Service[GoogleMapsModule] {
    override def googleMapsService(
      googleMapsContext: GoogleMapsContext
    ): ZIO[GoogleMapsModule, Throwable, GoogleMapsService] = ZIO.accessM[GoogleMapsModule](
      _.googleMapsModule.googleMapsService(googleMapsContext)
    )
  }

}

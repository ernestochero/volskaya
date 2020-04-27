package googleMaps

import commons.Constants.fastBiciCoordinate
import commons.VolskayaOperations.{
  buildGetPriceResponse,
  extractDistanceFromDistanceMatrix,
  validateCoordinateIntoArea
}
import googleMaps.model.{ DistanceMatrix, TravelMode, Units }
import models.Coordinate
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.VolskayaPrice
import zio._
import commons.Transformers._
package object googleMapsService {
  type GoogleMapsServiceType = Has[GoogleMapsService.Service]
  object GoogleMapsService {
    trait Service {
      def calculatePriceRoute(
        coordinateStart: Coordinate,
        coordinateFinish: Coordinate
      ): IO[VolskayaAPIException, VolskayaPrice]
    }
    def calculatePriceRoute(
      coordinateStart: Coordinate,
      coordinateFinish: Coordinate
    ): ZIO[GoogleMapsServiceType, VolskayaAPIException, VolskayaPrice] =
      ZIO.accessM[GoogleMapsServiceType](
        _.get.calculatePriceRoute(coordinateStart, coordinateFinish)
      )
    def make(googleMapsContext: GoogleMapsContext): ZLayer[Any, Nothing, GoogleMapsServiceType] =
      ZLayer.fromEffect {
        for {
          googleMapsContextRef <- Ref.make(googleMapsContext)
          subscribers          <- Ref.make(List.empty[Queue[String]])
        } yield
          new Service {
            def calculateDistanceMatrixFromCoordinates(
              coordinateStart: Coordinate,
              coordinateFinish: Coordinate
            ): Task[DistanceMatrix] =
              for {
                context <- googleMapsContextRef.get
                distanceMatrix <- DistanceMatrixApi
                  .getDistanceMatrix(
                    origins = coordinateStart.toString :: Nil,
                    destinations = coordinateFinish.toString :: Nil,
                    units = Some(Units.metric),
                    mode = Some(TravelMode.walking),
                    context = context
                  )
                  .toTask
              } yield distanceMatrix

            override def calculatePriceRoute(
              coordinateStart: Coordinate,
              coordinateFinish: Coordinate
            ): IO[VolskayaAPIException, VolskayaPrice] =
              for {
                _ <- ZIO.when(
                  !(validateCoordinateIntoArea(coordinateStart) && validateCoordinateIntoArea(
                    coordinateFinish
                  ))
                )(
                  ZIO.fail(
                    VolskayaAPIException("Delivery out of range")
                  )
                )
                distanceFastBiciToPickUpLocation <- calculateDistanceMatrixFromCoordinates(
                  fastBiciCoordinate,
                  coordinateStart
                ).mapError(err => VolskayaAPIException(err.getMessage))
                distancePickUptoLeaveLocation <- calculateDistanceMatrixFromCoordinates(
                  coordinateStart,
                  coordinateFinish
                ).mapError(err => VolskayaAPIException(err.getMessage))
                initialDistance <- extractDistanceFromDistanceMatrix(
                  distanceFastBiciToPickUpLocation
                )
                secondDistance <- extractDistanceFromDistanceMatrix(distancePickUptoLeaveLocation)
                priceResponse  <- buildGetPriceResponse(initialDistance, secondDistance)
              } yield priceResponse
          }
      }
  }
}

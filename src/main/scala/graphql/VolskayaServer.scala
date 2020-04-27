package graphql
import caliban.schema.GenericSchema
import caliban.Http4sAdapter
import cats.effect.Blocker
import googleMaps.GoogleMapsContext
import zio.{ RIO, Task, ZIO, ZLayer }
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.interop.catz._
import org.http4s.implicits._
import org.http4s.server.{ Router, ServiceErrorHandler }
import org.http4s.server.blaze.BlazeServerBuilder
import zio.blocking.Blocking
import commons.Logger._

import scala.language.higherKinds
import configuration.configurationService._
import logging.loggingService._
import googleMaps.googleMapsService._
import models.AuthServiceException._
import models.User
import mongodb.Mongo
import org.http4s.util.CaseInsensitiveString
import zio.interop.catz.implicits._
import userCollection.UserCollectionService
import userCollection.UserCollectionService.UserCollectionServiceType
import commons.JwtUtils._
import org.http4s.dsl.Http4sDsl
object VolskayaServer extends CatsApp with GenericSchema[Console with Clock] {
  type VolskayaTask[A] =
    RIO[Console with Clock with UserCollectionServiceType with GoogleMapsServiceType, A]
  object dsl extends Http4sDsl[Task]
  import dsl._
  val errorHandler: ServiceErrorHandler[Task] = _ => {
    case MissingToken() => Forbidden()
    case InvalidToken() => BadRequest()
  }
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = program
  val logic: ZIO[zio.ZEnv with ConfigurationServiceType with LoggingServiceType, Nothing, Int] =
    (for {
      conf <- ConfigurationService.buildConfiguration
      userCollection <- Mongo.setupMongoConfiguration[User](
        conf.mongoConf.uri,
        conf.mongoConf.database,
        conf.mongoConf.userCollection
      )
      _ <- LoggingService.info(s"init the graphql application ${conf.appName}")
      _ = logger.info(s"this is a common graphql logger application ${conf.appName}")
      googleMapsLayer = GoogleMapsService.make(
        GoogleMapsContext(apiKey = conf.googleMapsConf.apiKey)
      )
      userCollectionLayer = UserCollectionService.make(userCollection)
      _ <- for {
        _           <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
        interpreter <- VolskayaAPI.api.interpreter
        layers = googleMapsLayer ++ userCollectionLayer
        routeApi = Http4sAdapter.provideLayerFromRequest(
          Http4sAdapter.makeHttpService(interpreter),
          _.headers.get(CaseInsensitiveString("jwt-token")) match {
            case Some(extractedToken) =>
              decodeJwtToken(extractedToken.value, conf.jwtConf.secretKey).fold(
                _ => ZLayer.fail(InvalidToken()),
                _ => layers
              )
            case None => ZLayer.fail(MissingToken())
          }
        )
        routeWs = Http4sAdapter.provideLayerFromRequest(
          Http4sAdapter.makeWebSocketService(interpreter),
          _ => layers
        )
        _ <- BlazeServerBuilder[Task]
          .bindHttp(conf.httpConf.port, conf.httpConf.host)
          .withServiceErrorHandler(errorHandler)
          .withHttpApp(
            Router[Task](
              "/api/graphql" -> routeApi,
              "/ws/graphql"  -> routeWs,
            ).orNotFound
          )
          .resource
          .toManaged
          .useForever
      } yield 0
    } yield 0).catchAll(err => putStrLn(err.toString).as(1))

  val liveEnvironment = zio.ZEnv.live ++ ConfigurationService.live ++ LoggingService.live
  private val program = logic.provideLayer(liveEnvironment)
}

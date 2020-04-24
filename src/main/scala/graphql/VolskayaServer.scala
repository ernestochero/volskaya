package graphql
import caliban.schema.GenericSchema
import caliban.Http4sAdapter
import cats.effect.Blocker
import googleMaps.GoogleMapsContext
import zio.{ RIO, ZEnv, ZIO }
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.interop.catz._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.blocking.Blocking
import commons.Logger._
import scala.language.higherKinds
import configuration.configurationService._
import logging.loggingService._
import googleMaps.googleMapsService._
import models.User
import mongodb.Mongo
import userCollection.UserCollectionService
object VolskayaServer extends CatsApp with GenericSchema[Console with Clock] {
  type VolskayaTask[A] = RIO[ZEnv, A]
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
        _ <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
        interpreter <- VolskayaAPI.api.interpreter.map(
          _.provideCustomLayer(userCollectionLayer ++ googleMapsLayer)
        )
        _ <- BlazeServerBuilder[VolskayaTask]
          .bindHttp(conf.httpConf.port, conf.httpConf.host)
          .withHttpApp(
            Router[VolskayaTask](
              "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(interpreter)),
              "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter)),
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

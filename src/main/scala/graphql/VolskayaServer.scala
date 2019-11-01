package graphql
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import caliban.GraphQL._
import caliban.{ Http4sAdapter, RootResolver }
import modules.ConfigurationModule
import org.mongodb.scala.bson.ObjectId
import zio.{ RIO, ZIO }
import zio.clock.Clock
import zio.console.{ Console, putStrLn }
import zio.interop.catz._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.blocking.Blocking
import zio.random.Random
import zio.system.System
import scala.language.higherKinds
object VolskayaServer extends CatsApp with GenericSchema[Console with Clock] {
  type VolskayaTask[A] = RIO[Console with Clock, A]
  implicit val objectIdSchema     = Schema.stringSchema.contramap[ObjectId](_.toHexString)
  implicit val objectIdArgBuilder = ArgBuilder.string.map(new ObjectId(_))

  val logic: ZIO[zio.ZEnv with UserCollectionModule with ConfigurationModule, Nothing, Int] = (for {
    configuration <- ConfigurationModule.factory.configuration
    userCollection <- UserCollectionModule.factory.userCollection(
      configuration.mongoConf.uri,
      configuration.mongoConf.database,
      configuration.mongoConf.userCollection
    )
    service <- VolskayaService.make(userCollection)
    interpreter = graphQL(
      RootResolver(
        Queries(
          args => service.getUser(args.id),
          args => service.getAllUsers(args.limit, args.offset),
          service.wakeUpVolskaya
        ),
        Mutations(
          args =>
            service.updatePassword(
              args.id,
              args.oldPassword,
              args.newPassword
          )
        )
      )
    )
    _ <- BlazeServerBuilder[VolskayaTask]
      .bindHttp(configuration.httpConf.port, configuration.httpConf.host)
      .withHttpApp(
        Router(
          "/api/graphql" -> CORS(Http4sAdapter.makeRestService(interpreter)),
          "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter))
        ).orNotFound
      )
      .resource
      .toManaged
      .useForever
  } yield 0).catchAll(err => putStrLn(err.toString).as(1))

  private val program = logic.provideSome[zio.ZEnv] { env =>
    new System with Clock with Console with Blocking with Random with ConfigurationModule.Live
    with UserCollectionModule.Live {
      override val system: System.Service[Any]     = env.system
      override val clock: Clock.Service[Any]       = env.clock
      override val console: Console.Service[Any]   = env.console
      override val blocking: Blocking.Service[Any] = env.blocking
      override val random: Random.Service[Any]     = env.random
    }
  }
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = program
}

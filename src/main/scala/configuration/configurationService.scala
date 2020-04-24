package configuration

import pureconfig.ConfigSource
import zio.{ Has, Layer, ZIO, ZLayer }
import pureconfig.generic.auto._
package object configurationService {
  type ConfigurationServiceType = Has[ConfigurationService.Service]
  object ConfigurationService {
    case class ConfigurationError(message: String) extends RuntimeException(message)
    case class HttpConf(host: String, port: Int)
    case class MongoConf(database: String, uri: String, userCollection: String)
    case class GoogleMapsConf(apiKey: String)
    case class Configuration(appName: String,
                             httpConf: HttpConf,
                             mongoConf: MongoConf,
                             googleMapsConf: GoogleMapsConf)
    trait Service {
      def buildConfiguration: ZIO[ConfigurationServiceType, Throwable, Configuration]
    }

    def buildConfiguration: ZIO[ConfigurationServiceType, Throwable, Configuration] =
      ZIO.accessM[ConfigurationServiceType](_.get.buildConfiguration)

    val live: Layer[Nothing, ConfigurationServiceType] = ZLayer.succeed {
      new Service {
        override def buildConfiguration: ZIO[ConfigurationServiceType, Throwable, Configuration] =
          ZIO
            .fromEither(
              ConfigSource.default.load[Configuration]
            )
            .mapError(e => ConfigurationError(e.toList.mkString(", ")))
      }
    }
  }
}

package logging

import org.log4s.Logger
import zio._

package object loggingService {
  type LoggingServiceType = Has[LoggingService.Service]
  object LoggingService {
    trait Service {
      def debug(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit]
      def info(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit]
      def warn(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit]
      def warn(msg: String, exception: Throwable)(
        implicit logger: Logger
      ): ZIO[LoggingServiceType, Nothing, Unit]
      def error(msg: String, exception: Throwable)(
        implicit logger: Logger
      ): ZIO[LoggingServiceType, Nothing, Unit]
    }

    def debug(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
      ZIO.accessM[LoggingServiceType](_.get.debug(msg))

    def info(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
      ZIO.accessM[LoggingServiceType](_.get.info(msg))

    def warn(msg: String)(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
      ZIO.accessM[LoggingServiceType](_.get.warn(msg))

    def warn(msg: String, exception: Throwable)(
      implicit logger: Logger
    ): ZIO[LoggingServiceType, Nothing, Unit] =
      ZIO.accessM[LoggingServiceType](_.get.warn(msg, exception))

    def error(msg: String, exception: Throwable)(
      implicit logger: Logger
    ): ZIO[LoggingServiceType, Nothing, Unit] =
      ZIO.accessM[LoggingServiceType](_.get.error(msg, exception))

    val live: Layer[Nothing, LoggingServiceType] =
      ZLayer.succeed {
        new Service {
          override def debug(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
            UIO.effectTotal(logger.debug(msg))
          override def info(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
            UIO.effectTotal(logger.info(msg))
          override def warn(
            msg: String
          )(implicit logger: Logger): ZIO[LoggingServiceType, Nothing, Unit] =
            UIO.effectTotal(logger.warn(msg))
          override def warn(msg: String, exception: Throwable)(
            implicit logger: Logger
          ): ZIO[LoggingServiceType, Nothing, Unit] =
            UIO.effectTotal(logger.warn(exception)(msg))
          override def error(msg: String, exception: Throwable)(
            implicit logger: Logger
          ): ZIO[LoggingServiceType, Nothing, Unit] =
            UIO.effectTotal(logger.error(exception)(msg))
        }
      }
  }
}

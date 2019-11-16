package commons

import zio.{ RIO, ZIO }

import scala.concurrent.Future

object Transformers {
  implicit final class FutureConverter[V](val self: Future[V]) {
    def toRIO: RIO[Any, V] =
      ZIO.fromFuture(implicit ec => self)
  }
}

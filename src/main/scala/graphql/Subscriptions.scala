package graphql
import caliban.schema.Annotations.GQLDescription
import zio.stream.ZStream
import zio.console.Console
case class Subscriptions(
  @GQLDescription("Volskaya notify to you when a new coordinate is inserted")
  coordinateInserted: ZStream[Console, Nothing, String]
)

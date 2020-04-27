package graphql

import caliban.{ GraphQL, RootResolver }
import caliban.GraphQL.graphQL
import caliban.schema.{ ArgBuilder, GenericSchema, Schema }
import userCollection.UserCollectionService
import googleMaps.googleMapsService._
import userCollection.UserCollectionService.UserCollectionServiceType
import org.mongodb.scala.bson.ObjectId
import scala.language.higherKinds
object VolskayaAPI extends GenericSchema[UserCollectionServiceType with GoogleMapsServiceType] {
  implicit val objectIdSchema     = Schema.stringSchema.contramap[ObjectId](_.toHexString)
  implicit val objectIdArgBuilder = ArgBuilder.string.map(new ObjectId(_))

  val api: GraphQL[UserCollectionServiceType with GoogleMapsServiceType] =
    graphQL(
      RootResolver(
        Queries(
          UserCollectionService.wakeUpHeroku,
          args => GoogleMapsService.calculatePriceRoute(args.coordinateStart, args.coordinateFinish)
        )
      )
    )
}

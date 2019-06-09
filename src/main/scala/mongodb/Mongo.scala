package mongodb

import com.typesafe.config.ConfigFactory
import models._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.{DEFAULT_CODEC_REGISTRY, Macros}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

object Mongo {
  lazy val userCodecProvider = Macros.createCodecProvider[User]()
  lazy val deviceCodecProvider = Macros.createCodecProvider[Device]()
  lazy val orderCodecProvider = Macros.createCodecProvider[Order]()
  lazy val goalCodecProvider = Macros.createCodecProvider[Goal]()
  lazy val goalCanceledCodecProvider = Macros.createCodecProvider[GoalCanceled]()
  lazy val coordinateCodecProvider = Macros.createCodecProvider[Coordinate]()
  lazy val productCodecProvider = Macros.createCodecProvider[Product]()
  lazy val addFavoriteSiteCodecProvider = Macros.createCodecProvider[FavoriteSite]()
  lazy val personalInformationCodecProvider = Macros.createCodecProvider[PersonalInformation]()
  lazy val config = ConfigFactory.load()
  lazy val mongoClient: MongoClient = MongoClient(config.getString("mongo.uri"))
  lazy val codecRegistry = fromRegistries(
    fromProviders(userCodecProvider, deviceCodecProvider, personalInformationCodecProvider,
      orderCodecProvider, addFavoriteSiteCodecProvider, goalCodecProvider, goalCanceledCodecProvider, coordinateCodecProvider, productCodecProvider),
    DEFAULT_CODEC_REGISTRY)
  lazy val database: MongoDatabase = mongoClient.getDatabase(config.getString("mongo.database")).withCodecRegistry(codecRegistry)

  lazy val usersCollection: MongoCollection[User] = database.getCollection[User]("users")

}

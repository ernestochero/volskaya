package mongodb

import com.typesafe.config.ConfigFactory
import models._
import org.bson.codecs.configuration.{ CodecProvider, CodecRegistry }
import org.bson.{ BsonInvalidOperationException, BsonReader, BsonWriter }
import org.bson.codecs.{ Codec, DecoderContext, EncoderContext }
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.bson.codecs.{ DEFAULT_CODEC_REGISTRY, Macros }
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import zio.UIO

import scala.reflect.ClassTag

object Mongo {
  lazy val userCodecProvider                = Macros.createCodecProvider[User]()
  lazy val deviceCodecProvider              = Macros.createCodecProvider[Device]()
  lazy val orderCodecProvider               = Macros.createCodecProvider[Order]()
  lazy val goalCodecProvider                = Macros.createCodecProvider[Goal]()
  lazy val goalCanceledCodecProvider        = Macros.createCodecProvider[GoalCanceled]()
  lazy val coordinateCodecProvider          = Macros.createCodecProvider[Coordinate]()
  lazy val productCodecProvider             = Macros.createCodecProvider[Product]()
  lazy val addFavoriteSiteCodecProvider     = Macros.createCodecProvider[FavoriteSite]()
  lazy val personalInformationCodecProvider = Macros.createCodecProvider[PersonalInformation]()
  lazy val addressCodecProvider             = Macros.createCodecProvider[Address]()
  lazy val orderStateCodecProvider          = Macros.createCodecProvider[OrderState]()
  lazy val finalClientCodecProvider         = Macros.createCodecProvider[FinalClient]()
  lazy val routeCodecProvider               = Macros.createCodecProvider[Route]()
  lazy val companyInformationCodecProvider  = Macros.createCodecProvider[CompanyInformation]()
  lazy val userAuthenticateCodecProvider    = Macros.createCodecProvider[UserAuthenticate]()
  lazy val payDefinitionCodecProvider       = Macros.createCodecProvider[PayDefinition]()
  lazy val roleEnumCodecProvider            = RoleEnumCodecProvider
  lazy val config                           = ConfigFactory.load()
  lazy val mongoClient: MongoClient         = MongoClient(config.getString("mongo.uri"))
  lazy val codecRegistry = fromRegistries(
    fromProviders(
      userCodecProvider,
      deviceCodecProvider,
      personalInformationCodecProvider,
      orderCodecProvider,
      addFavoriteSiteCodecProvider,
      goalCodecProvider,
      goalCanceledCodecProvider,
      coordinateCodecProvider,
      productCodecProvider,
      addressCodecProvider,
      orderStateCodecProvider,
      finalClientCodecProvider,
      routeCodecProvider,
      companyInformationCodecProvider,
      userAuthenticateCodecProvider,
      payDefinitionCodecProvider,
      roleEnumCodecProvider
    ),
    DEFAULT_CODEC_REGISTRY
  )
  lazy val database: MongoDatabase =
    mongoClient
      .getDatabase(config.getString("mongo-conf.database"))
      .withCodecRegistry(codecRegistry)

  lazy val usersCollection: MongoCollection[User]   = database.getCollection[User]("users")
  lazy val ordersCollection: MongoCollection[Order] = database.getCollection[Order]("orders")

  def mongoClient2(uri: String): UIO[MongoClient] = UIO.succeed(MongoClient(uri))
  def database2(dbname: String, mongoClient: MongoClient): UIO[MongoDatabase] =
    UIO.succeed((mongoClient.getDatabase(dbname).withCodecRegistry(codecRegistry)))
  def collection2[T](db: MongoDatabase,
                     collectionName: String)(implicit c: ClassTag[T]): UIO[MongoCollection[T]] =
    UIO.succeed(db.getCollection[T](collectionName))

  def setupMongoConfiguration[T](uri: String, databaseName: String, collectionName: String)(
    implicit c: ClassTag[T]
  ): UIO[MongoCollection[T]] =
    for {
      mongoClient <- mongoClient2(uri)
      database    <- database2(databaseName, mongoClient)
      collection  <- collection2[T](database, collectionName)
    } yield collection
}

object RoleEnumCodecProvider extends CodecProvider {
  def isCaseObjectEnum[T](clazz: Class[T]): Boolean =
    clazz.isInstance(Role.Client) || clazz.isInstance(Role.Company) || clazz.isInstance(
      Role.DeliveryCompany
    ) || clazz.isInstance(Role.DeliveryPerson)
  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] =
    if (isCaseObjectEnum(clazz)) RoleEnumCodec.asInstanceOf[Codec[T]]
    else null

  object RoleEnumCodec extends Codec[Role] {
    val identifier = "_t"
    override def encode(writer: BsonWriter, value: Role, encoderContext: EncoderContext): Unit = {
      val roleName = value match {
        case Role.Client          => "Client"
        case Role.Company         => "Company"
        case Role.DeliveryCompany => "DeliveryCompany"
        case Role.DeliveryPerson  => "DeliveryPerson"
      }
      writer.writeStartDocument()
      writer.writeString(identifier, roleName)
      writer.writeEndDocument()
    }

    override def getEncoderClass: Class[Role] = Role.getClass.asInstanceOf[Class[Role]]
    override def decode(reader: BsonReader, decoderContext: DecoderContext): Role = {
      reader.readStartDocument()
      val roleName = reader.readString(identifier)
      reader.readEndDocument()
      roleName match {
        case "Client"          => Role.Client
        case "Company"         => Role.Company
        case "DeliveryCompany" => Role.DeliveryCompany
        case "DeliveryPerson"  => Role.DeliveryPerson
        case _ =>
          throw new BsonInvalidOperationException(
            s"$roleName is an invalid value for a role object"
          )
      }
    }
  }

}

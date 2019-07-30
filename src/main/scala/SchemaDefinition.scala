import akka.stream.Materializer
import akka.util.Timeout
import models.OrderManagementEvents.OrderEventCreated
import models.VolskayaMessages._
import models._
import play.api.libs.json.Json
import sangria.schema._
import sangria.marshalling.playJson._
import sangria.macros.derive._
import volskayaSystem.VolskayaController
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext

object SchemaDefinition {

  /* type from classes */

  def createSchema(implicit timeout: Timeout,
                   ec: ExecutionContext,
                   mat: Materializer): Schema[VolskayaController, Unit] = {
    implicit val ProductType   = deriveObjectType[Unit, Product]()
    implicit val productFormat = Json.format[Product]
    implicit val ProductInputType =
      deriveInputObjectType[Product](InputObjectTypeName("ProductFieldsInput"))

    implicit val CoordinateType   = deriveObjectType[Unit, Coordinate]()
    implicit val coordinateFormat = Json.format[Coordinate]
    implicit val CoordinateInputType =
      deriveInputObjectType[Coordinate](InputObjectTypeName("CoordinateFieldsInput"))

    implicit val GoalCanceledType   = deriveObjectType[Unit, GoalCanceled]()
    implicit val goalCanceledFormat = Json.format[GoalCanceled]
    implicit val GoalCanceledInputType =
      deriveInputObjectType[GoalCanceled](InputObjectTypeName("GoalCanceledFieldsInput"))

    implicit val GoalType      = deriveObjectType[Unit, Goal]()
    implicit val goalFormat    = Json.format[Goal]
    implicit val GoalInputType = deriveInputObjectType[Goal](InputObjectTypeName("GoalFieldsInput"))

    implicit val DeviceType   = deriveObjectType[Unit, Device]()
    implicit val deviceFormat = Json.format[Device]
    implicit val DeviceInputType =
      deriveInputObjectType[Device](InputObjectTypeName("DeviceFieldsInput"))

    implicit val PersonalInformationType   = deriveObjectType[Unit, PersonalInformation]()
    implicit val personalInformationFormat = Json.format[PersonalInformation]
    implicit val PersonalInformationInputType = deriveInputObjectType[PersonalInformation](
      InputObjectTypeName("PersonalInformationFieldsInput")
    )

    implicit val FavoriteSiteType   = deriveObjectType[Unit, FavoriteSite]()
    implicit val FavoriteSiteFormat = Json.format[FavoriteSite]
    implicit val FavoriteSiteInputType =
      deriveInputObjectType[FavoriteSite](InputObjectTypeName("FavoriteSiteFieldsInput"))

    implicit val UserType   = deriveObjectType[Unit, UserDomain]()
    implicit val userFormat = Json.format[UserDomain]
    implicit val UserInputType =
      deriveInputObjectType[UserDomain](InputObjectTypeName("UserFieldsInput"))

    implicit val OrderStateTType = deriveEnumType[OrderStateT](EnumTypeName("OrderStateT"))

    implicit val PayMethodType = deriveEnumType[PayMethod](EnumTypeName("PayMethod"))

    implicit val OrderStateType   = deriveObjectType[Unit, OrderState]()
    implicit val orderStateFormat = Json.format[OrderState]
    implicit val OrderStateInputType =
      deriveInputObjectType[OrderState](InputObjectTypeName("OrderStateFieldsInput"))

    implicit val AddressType   = deriveObjectType[Unit, Address]()
    implicit val addressFormat = Json.format[Address]
    implicit val AddressInputType =
      deriveInputObjectType[Address](InputObjectTypeName("AddressFieldsInput"))

    implicit val RouteType   = deriveObjectType[Unit, Route]()
    implicit val routeFormat = Json.format[Route]
    implicit val RouteInputType =
      deriveInputObjectType[Route](InputObjectTypeName("RouteFieldsInput"))

    implicit val FinalClientType   = deriveObjectType[Unit, FinalClient]()
    implicit val finalClientFormat = Json.format[FinalClient]
    implicit val FinalClientInputType = deriveInputObjectType[FinalClient](
      InputObjectTypeName("FinalClientFieldsInput")
    )

    implicit val OrderType   = deriveObjectType[Unit, OrderDomain]()
    implicit val orderFormat = Json.format[OrderDomain]
    implicit val OrderInputType =
      deriveInputObjectType[OrderDomain](InputObjectTypeName("OrderFieldsInput"))

    /* custom types*/

    implicit val VolskayaMessageInterfaceType = InterfaceType(
      "volskayaMessageInterface",
      "volskaya Message Interface Description",
      () =>
        fields[Unit, VolskayaMessage](
          Field("message", StringType, resolve = _.value.message)
      )
    )

    implicit val VolskayaMessageResponseType = ObjectType(
      "volskayaMessageOutputType",
      "Format to return some request",
      interfaces[Unit, VolskayaResponse](VolskayaMessageInterfaceType),
      fields[Unit, VolskayaResponse](
        Field("responseCode", StringType, resolve = _.value.responseCode),
        Field("responseMessage", StringType, resolve = _.value.responseMessage)
      )
    )

    implicit val VolskayaMessagePriceResponseType = ObjectType(
      "volskayaMessagePriceOutputType",
      "Format to return getPrice request",
      fields[Unit, VolskayaGetPriceResponse](
        Field("price", OptionType(FloatType), resolve = _.value.price),
        Field("distance", OptionType(IntType), resolve = _.value.distance),
        Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
      )
    )

    implicit val VolskayaMessageUserResponseType = ObjectType(
      "volskayaMessageUserOutputType",
      "Format to return getUser request",
      fields[Unit, VolskayaGetUserResponse](
        Field("user", OptionType(UserType), resolve = _.value.userDomain),
        Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
      )
    )

    implicit val VolskayaMessageLoginResponseType = ObjectType(
      "volskayaMessageLoginOutputType",
      "Format to return login request",
      fields[Unit, VolskayaLoginResponse](
        Field("id", OptionType(StringType), resolve = _.value.id),
        Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
      )
    )

    implicit val VolskayaMessageRegisterResponseType = ObjectType(
      "volskayaMessageRegisterOutputType",
      "Format to return register request",
      fields[Unit, VolskayaRegisterResponse](
        Field("id", OptionType(StringType), resolve = _.value.id),
        Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
      )
    )

    implicit val VolskayaMessageOrderResponseType = ObjectType(
      "volskayaMessageOrderOutputType",
      "Format to return getORder request",
      fields[Unit, VolskayaGetOrderResponse](
        Field("order", OptionType(OrderType), resolve = _.value.orderDomain),
        Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
      )
    )

    val EventType = InterfaceType(
      "Event",
      "Event InterfaceType Description",
      () =>
        fields[Unit, Event](
          Field("id", StringType, resolve = _.value.id)
      )
    )

    val OrderEventCreatedType = deriveObjectType[Unit, OrderEventCreated](
      Interfaces(EventType)
    )

    val LimitArg  = Argument("limit", OptionInputType(IntType), defaultValue = 20)
    val OffsetArg = Argument("offset", OptionInputType(IntType), defaultValue = 0)

    val userFieldQueries = fields[VolskayaController, Unit](
      Field(
        "allUsers",
        ListType(UserType),
        description = Some("Returns a list of all available users."),
        arguments = LimitArg :: OffsetArg :: Nil,
        resolve = context => {
          context.ctx.getAllUsers(context.arg(LimitArg), context.arg(OffsetArg))
        }
      ),
      Field(
        "getUser",
        VolskayaMessageUserResponseType,
        description = Some("Return a specific User by ID"),
        arguments = Argument("id", StringType) :: Nil,
        resolve = context => {
          context.ctx.getUser(context.arg("id"))
        }
      ),
      Field(
        "login",
        VolskayaMessageUserResponseType,
        arguments = Argument("email", StringType) :: Argument("password", StringType) :: Nil,
        resolve = context => {
          context.ctx.verifyLogin(context.arg("email"), context.arg("password"))
        }
      ),
      Field(
        "checkCode",
        VolskayaMessageResponseType,
        arguments = Argument("id", StringType) :: Argument("code", StringType) :: Nil,
        resolve = context => {
          context.ctx.checkCode(context.arg("id"), context.arg("code"))
        }
      ),
      Field(
        "getPrice",
        VolskayaMessagePriceResponseType,
        description = Some("Return a price of one Route"),
        arguments = Argument("coordinateStart", CoordinateInputType) :: Argument(
          "coordinateFinish",
          CoordinateInputType
        ) :: Nil,
        resolve = context => {
          context.ctx.calculatePriceRoute(context.arg("coordinateStart"),
                                          context.arg("coordinateFinish"))
        }
      )
    )

    val ordersFieldQueries = fields[VolskayaController, Unit](
      Field(
        "allOrders",
        ListType(OrderType),
        description = Some("Returns a list of all available orders."),
        arguments = LimitArg :: OffsetArg :: Nil,
        resolve = context => {
          context.ctx.getAllOrders(context.arg(LimitArg), context.arg(OffsetArg))
        }
      ),
      Field(
        "getOrder",
        VolskayaMessageOrderResponseType,
        description = Some("Return a specific User by ID"),
        arguments = Argument("id", StringType) :: Nil,
        resolve = context => {
          context.ctx.getOrder(context.arg("id"))
        }
      )
    )

    val userFieldMutations = fields[VolskayaController, Unit](
      Field(
        "updatePassword",
        VolskayaMessageResponseType,
        arguments = Argument("id", StringType)
        :: Argument("oldPassword", StringType)
        :: Argument("newPassword", StringType) :: Nil,
        resolve = context => {
          context.ctx.updatePassword(context.arg("id"),
                                     context.arg("oldPassword"),
                                     context.arg("newPassword"))
        }
      ),
      Field(
        "addFavoriteSite",
        VolskayaMessageResponseType,
        arguments = Argument("id", StringType)
        :: Argument("favoriteSite", FavoriteSiteInputType)
        :: Nil,
        resolve = context => {
          context.ctx.addFavoriteSite(context.arg("id"), context.arg("favoriteSite"))
        }
      ),
      Field(
        "register",
        VolskayaMessageUserResponseType,
        arguments = Argument("email", StringType)
        :: Argument("password", StringType)
        :: Argument("phoneNumber", StringType) :: Nil,
        resolve = context => {
          context.ctx.registerUser(context.arg("email"),
                                   context.arg("password"),
                                   context.arg("phoneNumber"))
        }
      )
    )

    val orderFieldMutations = fields[VolskayaController, Unit](
      Field(
        "registerOrder",
        VolskayaMessageRegisterResponseType,
        arguments = Argument("route", RouteInputType)
        :: Argument("clientId", StringType)
        :: Argument("finalClient", FinalClientInputType)
        :: Argument("products", ListInputType(ProductInputType))
        :: Argument("price", FloatType)
        :: Argument("distance", FloatType)
        :: Argument("generalDescription", StringType) :: Nil,
        resolve = context => {
          context.ctx.registerOrder(
            context.arg("route"),
            context.arg("clientId"),
            context.arg("finalClient"),
            context.arg("products"),
            context.arg("price"),
            context.arg("distance"),
            context.arg("generalDescription")
          )
        }
      )
    )

    val QueryType = ObjectType("Query", userFieldQueries ++ ordersFieldQueries)

    val mutationFields = userFieldMutations ++ orderFieldMutations

    val MutationType = ObjectType("Mutation", mutationFields)

    /** creates a subscription field for a specific event type */
    def subscriptionField[T <: Event](
      tpe: ObjectType[VolskayaController, T]
    ): Field[VolskayaController, Unit] = {
      val fieldName = tpe.name.head.toLower + tpe.name.tail

      Field.subs(
        fieldName,
        tpe,
        resolve = (c: Context[VolskayaController, Unit]) =>
          c.ctx.eventStream
            .filter(event => tpe.valClass.isAssignableFrom(event.getClass))
            .map(event => Action(event.asInstanceOf[T]))
      )
    }

    val SubscriptionType = ObjectType(
      "Subscription",
      fields[VolskayaController, Unit](
        subscriptionField(OrderEventCreatedType),
        Field.subs(
          "allEvents",
          EventType,
          resolve = _.ctx.eventStream.map(Action(_))
        )
      )
    )

    Schema(QueryType, Some(MutationType), Some(SubscriptionType))
  }
}

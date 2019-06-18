import models.VolskayaMessages._
import models._
import play.api.libs.json.Json
import repository.UserRepo
import sangria.schema._
import sangria.marshalling.playJson._
import sangria.macros.derive.{InputObjectTypeName, deriveInputObjectType, deriveObjectType}
import user.{UserManagerAPI}
object SchemaDefinition {

  /* type from classes */

  implicit val ProductType = deriveObjectType[Unit, Product]()
  implicit val productFormat = Json.format[Product]
  implicit val ProductInputType = deriveInputObjectType[Product](InputObjectTypeName("ProductFieldsInput"))

  implicit val CoordinateType = deriveObjectType[Unit, Coordinate]()
  implicit val coordinateFormat = Json.format[Coordinate]
  implicit val CoordinateInputType = deriveInputObjectType[Coordinate](InputObjectTypeName("CoordinateFieldsInput"))

  implicit val GoalCanceledType = deriveObjectType[Unit, GoalCanceled]()
  implicit val goalCanceledFormat = Json.format[GoalCanceled]
  implicit val GoalCanceledInputType = deriveInputObjectType[GoalCanceled](InputObjectTypeName("GoalCanceledFieldsInput"))

  implicit val GoalType = deriveObjectType[Unit, Goal]()
  implicit val goalFormat = Json.format[Goal]
  implicit val GoalInputType = deriveInputObjectType[Goal](InputObjectTypeName("GoalFieldsInput"))

  implicit val OrderType = deriveObjectType[Unit, Order]()
  implicit val orderFormat = Json.format[Order]
  implicit val OrderInputType = deriveInputObjectType[Order](InputObjectTypeName("OrderFieldsInput"))

  implicit val DeviceType = deriveObjectType[Unit, Device]()
  implicit val deviceFormat  = Json.format[Device]
  implicit val DeviceInputType = deriveInputObjectType[Device](InputObjectTypeName("DeviceFieldsInput"))

  implicit val PersonalInformationType = deriveObjectType[Unit, PersonalInformation]()
  implicit val personalInformationFormat = Json.format[PersonalInformation]
  implicit val PersonalInformationInputType = deriveInputObjectType[PersonalInformation](InputObjectTypeName("PersonalInformationFieldsInput"))

  implicit val FavoriteSiteType = deriveObjectType[Unit, FavoriteSite]()
  implicit val FavoriteSiteFormat = Json.format[FavoriteSite]
  implicit val FavoriteSiteInputType = deriveInputObjectType[FavoriteSite](InputObjectTypeName("FavoriteSiteFieldsInput"))

  implicit val UserType = deriveObjectType[Unit, UserDomain]()
  implicit val userFormat = Json.format[UserDomain]
  implicit val UserInputType = deriveInputObjectType[UserDomain](InputObjectTypeName("UserFieldsInput"))

  implicit val RouteType = deriveObjectType[Unit, Route]()
  implicit val routeFormat = Json.format[Route]
  implicit val RouteInputType = deriveInputObjectType[Route](InputObjectTypeName("RouteFieldsInput"))

  /* custom types*/

  implicit val VolskayaMessageInterfaceType = InterfaceType("volskayaMessageInterface","volskaya Message Interface Description",
    () => fields[Unit, VolskayaMessage](
      Field("message", StringType, resolve =  _.value.message)
    ))

  implicit val VolskayaMessageResponseType = ObjectType("volskayaMessageOutputType","Format to return some request",
    interfaces[Unit, VolskayaResponse](VolskayaMessageInterfaceType),
    fields[Unit, VolskayaResponse](
      Field("responseCode", StringType, resolve = _.value.responseCode),
      Field("responseMessage", StringType, resolve = _.value.responseMessage)
    ))

  implicit val VolskayaMessagePriceResponseType = ObjectType("volskayaMessagePriceOutputType", "Format to return getPrice request",
    fields[Unit, VolskayaGetPriceResponse](
      Field("price", OptionType(FloatType), resolve = _.value.price),
      Field("distance", OptionType(IntType), resolve = _.value.distance),
      Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
    ))

  implicit val VolskayaMessageUserResponseType = ObjectType("volskayaMessageUserOutputType", "Format to return getUser request",
    fields[Unit, VolskayaGetUserResponse](
     Field("user", OptionType(UserType), resolve = _.value.userDomain),
     Field("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
    ))

  implicit val VolskayaMessageLoginResponseType = ObjectType("volskayaMessageLoginOutputType", "Format to return login request",
    fields[Unit, VolskayaLoginResponse](
      Field("id", OptionType(StringType), resolve = _.value.id),
      Field ("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
    ))

  implicit val VolskayaMessageRegisterResponseType = ObjectType("volskayaMessageRegisterOutputType", "Format to return register request",
    fields[Unit, VolskayaRegisterResponse](
      Field("id", OptionType(StringType), resolve = _.value.id),
      Field ("volskayaResponse", VolskayaMessageResponseType, resolve = _.value.volskayaResponse)
    ))

  /* Arguments*/
  val IdArg = Argument("id", OptionInputType(StringType))
  val DeviceArg = Argument("device", OptionInputType(DeviceInputType))
  val PersonalInformationArg = Argument("personalInformation", OptionInputType(PersonalInformationInputType))
  val EmailArg = Argument("email", OptionInputType(StringType))
  val PasswordArg = Argument("password", OptionInputType(StringType))
  val IsAuthenticatedArg = Argument("isAuthenticated", OptionInputType(BooleanType))
  val OrdersArg = Argument("orders", OptionInputType(ListInputType(OrderInputType)))
  val RouteArg = Argument("route", RouteInputType)
  val FavoriteSitesArg = Argument("favoriteSites", OptionInputType(ListInputType(FavoriteSiteInputType)))
  val ConfirmationCodeArg = Argument("confirmationCode", OptionInputType(StringType))

  val arguments = List(
    IdArg,
    DeviceArg,
    PersonalInformationArg,
    EmailArg,
    PasswordArg,
    IsAuthenticatedArg,
    OrdersArg,
    FavoriteSitesArg,
    ConfirmationCodeArg
  )

  def buildUserDomain(context:Context[UserRepo, Unit]): UserDomain = {
    UserDomain(
      context.arg(IdArg),
      context.arg(DeviceArg),
      context.arg(PersonalInformationArg),
      context.arg(EmailArg),
      context.arg(PasswordArg),
      context.arg(IsAuthenticatedArg),
      context.arg(OrdersArg).map(_.toList),
      context.arg(FavoriteSitesArg).map(_.toList),
      context.arg(ConfirmationCodeArg)
    )
  }

  val LimitArg = Argument("limit", OptionInputType(IntType), defaultValue = 20)
  val OffsetArg = Argument("offset", OptionInputType(IntType), defaultValue = 0)

  val QueryType = ObjectType("Query", fields[UserManagerAPI, Unit](
    Field("allUsers", ListType(UserType),
      description = Some("Returns a list of all available users."),
      arguments = LimitArg :: OffsetArg :: Nil,
      resolve = context => {
        context.ctx.getAllUsers(context.arg(LimitArg), context.arg(OffsetArg))
      }
    ),
    Field("getUser", VolskayaMessageUserResponseType,
      description = Some("Return a specific User by ID"),
      arguments = Argument("id", StringType) :: Nil,
      resolve = context => {
        context.ctx.getUser(context.arg("id"))
      }
    ),
    Field("login",VolskayaMessageUserResponseType,
      arguments = Argument("email", StringType) :: Argument("password", StringType) :: Nil,
      resolve = context => {
        context.ctx.verifyLogin(context.arg("email"), context.arg("password"))
      }
    )
    /*,

    Field("getPrice", VolskayaMessagePriceResponseType,
      description = Some("Return a price of one Route"),
      arguments = Argument("coordinateStart", CoordinateInputType) :: Argument("coordinateFinish", CoordinateInputType) :: Nil,
      resolve = context => {
        context.ctx.calculatePriceRoute(context.arg("coordinateStart"), context.arg("coordinateFinish"))
      }
    ),
    Field("sendCode", VolskayaMessageResponseType,
      arguments = Argument("code", StringType) :: Argument("phoneNumber", StringType) :: Nil,
      resolve = context => {
        context.ctx.sendCode(context.arg("code"), context.arg("phoneNumber"))
      }
    ),
    Field("checkCode", VolskayaMessageResponseType,
      arguments = Argument("id", StringType) :: Argument("code", StringType) :: Nil,
      resolve = context => {
        context.ctx.checkCode(context.arg("id"), context.arg("code"))
      }
    )*/
  )
  )

  val MutationType = ObjectType("Mutation", fields[UserManagerAPI, Unit](
    Field("updatePassword",VolskayaMessageResponseType,
      arguments = Argument("id", StringType)
        :: Argument("oldPassword", StringType)
        :: Argument("newPassword", StringType) :: Nil,
      resolve = context => {
        context.ctx.updatePassword(context.arg("id"), context.arg("oldPassword"), context.arg("newPassword"))
      }
    )/*,
    Field("addUser", UserType,
      arguments = arguments,
      resolve = context => {
        val repo = context.ctx
        repo.saveUser(buildUserDomain(context))
      }
    ),
    Field("register", VolskayaMessageRegisterResponseType,
      arguments = Argument("email", StringType)
        :: Argument("password", StringType)
        :: Argument("phoneNumber", StringType) :: Nil,
      resolve = context => {
        context.ctx.register(context.arg("email"), context.arg("password"), context.arg("phoneNumber"))
      }
    ),
    Field("addFavoriteSite", VolskayaMessageResponseType,
      arguments = Argument("id", StringType)
        :: Argument("favoriteSite", FavoriteSiteInputType)
        :: Nil,
      resolve = context => {
        context.ctx.addFavoriteSite(context.arg("id"), context.arg("favoriteSite"))
      }
    )*/
   )
  )

  val UserSchema = Schema(QueryType, Some(MutationType))
}
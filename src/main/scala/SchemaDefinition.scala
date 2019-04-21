import models._
import play.api.libs.json.Json
import repository.UserRepo
import sangria.schema._
import sangria.marshalling.playJson._

object SchemaDefinition {

  implicit val ProductType = ObjectType("Product", "product description",
    fields[Unit, Product](
      Field("name", StringType, resolve = _.value.name),
      Field("description", StringType, resolve = _.value.description),
      Field("photo", OptionType(StringType), resolve = _.value.photo),
      Field("isEspecial", OptionType(BooleanType), resolve = _.value.isSpecial)
    ))

  implicit val productFormat = Json.format[Product]

  implicit val ProductInputType = InputObjectType[Product]("product", List(
    InputField("name", StringType),
    InputField("description", StringType),
    InputField("photo", StringType),
    InputField("isEspecial", OptionInputType(StringType))
  ))

  implicit val CoordinateType = ObjectType("Coordinate", "coordinate description",
    fields[Unit, Coordinate](
      Field("latitude", FloatType, resolve = _.value.latitude),
      Field("longitude", FloatType, resolve = _.value.longitude)
    ))

  implicit val coordinateFormat = Json.format[Coordinate]

  implicit val CoordinateInputType = InputObjectType[Coordinate]("Coordinate", List(
    InputField("latitude", FloatType),
    InputField("longitude", FloatType)
  ))

  implicit val GoalCanceledType = ObjectType("GoalCanceled", "goalCanceled description",
    fields[Unit, GoalCanceled](
      Field("time", OptionType(StringType), resolve = _.value.time),
      Field("reason", OptionType(StringType), resolve = _.value.reason)
    ))

  implicit val goalCanceledFormat = Json.format[GoalCanceled]

  implicit val GoalCanceledInputType = InputObjectType[GoalCanceled]("Goal Canceled", List(
    InputField("time", OptionInputType(StringType)),
    InputField("reason", OptionInputType(StringType))
  ))

  implicit val GoalType = ObjectType("Goal", "goal description",
    fields[Unit, Goal](
      Field("products", OptionType(ListType(ProductType)), resolve = _.value.products),
      Field("userCyclistId", OptionType(StringType), resolve = _.value.userCyclistId),
      Field("goalCoordinate", OptionType(CoordinateType), resolve = _.value.goalCoordinate),
      Field("goalTypeName", OptionType(StringType), resolve = _.value.goalTypeName),
      Field("goalCanceled", OptionType(GoalCanceledType), resolve = _.value.goalCanceled)
    ))

  implicit val goalFormat = Json.format[Goal]

  implicit val GoalInputType = InputObjectType[Goal]("Goal", List(
    InputField("products", OptionInputType(ListInputType(ProductInputType))),
    InputField("userCyclistId", OptionInputType(StringType)),
    InputField("goalTypeName", OptionInputType(CoordinateInputType)),
    InputField("goalCoordinate", OptionInputType(StringType)),
    InputField("goalCanceled", OptionInputType(GoalCanceledInputType))
  ))

  implicit val OrderType = ObjectType("Order", "order description",
    fields[Unit, Order](
      Field("orderTypeName", OptionType(StringType), resolve = _.value.orderTypeName),
      Field("statusOrderTypeName", OptionType(StringType), resolve = _.value.statusOrderTypeName),
      Field("kilometers", OptionType(FloatType), resolve = _.value.kilometers),
      Field("finalPrice", OptionType(FloatType), resolve = _.value.finalPrice),
      Field("isPaid", OptionType(BooleanType), resolve = _.value.isPaid),
      Field("paymentDateTime", OptionType(StringType), resolve = _.value.paymentDateTime),
      Field("paymentMethod", OptionType(StringType), resolve = _.value.paymentMethod),
      Field("goals",OptionType(ListType(GoalType)), resolve = _.value.goals)
    ))

  implicit val orderFormat = Json.format[Order]

  implicit val OrderInputType = InputObjectType[Order]("Order", List(
    InputField("orderTypeName", OptionInputType(StringType)),
    InputField("statusOrderTypeName", OptionInputType(StringType)),
    InputField("kilometers", OptionInputType(FloatType)),
    InputField("finalPrice", OptionInputType(FloatType)),
    InputField("isPaid", OptionInputType(BooleanType)),
    InputField("paymentDateTime", OptionInputType(StringType)),
    InputField("paymentMethod", OptionInputType(StringType)),
    InputField("goals", OptionInputType(ListInputType(GoalInputType)))
  ))


   /*
   *  Device
   * */

  implicit val DeviceType = ObjectType("Device", "device description",
    fields[Unit, Device](
      Field("name", StringType, resolve = _.value.name),
      Field("number", StringType, resolve = _.value.number),
      Field("imei", StringType, resolve = _.value.imei),
      Field("token", OptionType(StringType), resolve = _.value.token)
    ))

  implicit val deviceFormat  = Json.format[Device]

  implicit val DeviceInputType = InputObjectType[Device]("Device", List(
    InputField("name", StringType),
    InputField("number", StringType),
    InputField("imei", StringType),
    InputField("token", OptionInputType(StringType))
  ))


  /*
  * User
  **/

  implicit val UserProducerType = ObjectType("UserProducer", "userProducer description",
    fields[Unit, UserProducer](
      Field("nameCompany", StringType, resolve = _.value.nameCompany),
      Field("address", StringType, resolve = _.value.address),
      Field("phone", StringType, resolve = _.value.phone),
      Field("ruc", StringType, resolve = _.value.ruc)
    ))

  implicit val userProducerFormat = Json.format[UserProducer]

  implicit val UserProducerInputType = InputObjectType[UserProducer]("UserProducer", List(
    InputField("nameCompany", StringType),
    InputField("address", StringType),
    InputField("phone", StringType),
    InputField("ruc", StringType)
  ))


  implicit val UserCyclistType = ObjectType("UserCyclist", "userCyclist description",
    interfaces[Unit, UserCyclist](PersonType),
    fields[Unit, UserCyclist](
      Field("firstName", StringType, resolve = _.value.firstName ),
      Field("lastName", StringType, resolve = _.value.lastName),
      Field("dni", StringType, resolve = _.value.dni)
    ))

  implicit val userCyclistFormat  = Json.format[UserCyclist]

  implicit val UserCyclistInputType = InputObjectType[UserCyclist]("UserCyclist", List(
    InputField("firstName", StringType),
    InputField("lastName", StringType),
    InputField("dni", StringType)
  ))

  implicit val PersonType: InterfaceType[Unit, Person] = InterfaceType("Person", "person description",
    () => fields[Unit, Person](
      Field("firstName", StringType, resolve = _.value.firstName),
      Field("lastName", StringType, resolve = _.value.lastName),
      Field("dni", StringType, resolve = _.value.dni)
    ))

  implicit val UserType = ObjectType("User", "user description",
    fields[Unit, UserDomain](
      Field("id", OptionType(StringType), resolve = _.value.id),
      Field("device", DeviceType, resolve = _.value.device),
      Field("userCyclist", OptionType(UserCyclistType), resolve = _.value.userCyclist),
      Field("userProducer", OptionType(UserProducerType), resolve = _.value.userProducer),
      Field("email", StringType, resolve = _.value.email),
      Field("password", StringType, resolve = _.value.password),
      Field("isAuthenticated", BooleanType, resolve = _.value.isAuthenticated),
      Field("orders", OptionType(ListType(OrderType)), resolve = _.value.orders)
    ))

  val IdArg = Argument("id", OptionInputType(StringType))
  val DeviceArg = Argument("device", DeviceInputType)
  val UserCyclistArg = Argument("userCyclist", OptionInputType(UserCyclistInputType))
  val UserProducerArg = Argument("userProducer", OptionInputType(UserProducerInputType))
  val EmailArg = Argument("email", StringType)
  val PasswordArg = Argument("email", StringType)
  val IsAuthenticatedArg = Argument("isAuthenticated", BooleanType)
  val OrdersArg = Argument("orders", OptionInputType(ListInputType(OrderInputType)))

  val QueryType = ObjectType("Query", fields[UserRepo, Unit](
    Field("allUsers", ListType(UserType),
      description = Some("Returns a list of all available users."),
      resolve = _.ctx.allProducts
    )
   )
  )

  val MutationType = ObjectType("Mutation", fields[UserRepo, Unit](
    Field("addUser", UserType,
      arguments =
        List(
          IdArg,
          DeviceArg,
          UserCyclistArg,
          UserProducerArg,
          EmailArg,
          PasswordArg,
          IsAuthenticatedArg,
          OrdersArg
      ),
      resolve = context => {
        val repo = context.ctx
        val user = UserDomain(
          context.arg(IdArg),
          context.arg(DeviceArg),
          context.arg(UserCyclistArg),
          context.arg(UserProducerArg),
          context.arg(EmailArg),
          context.arg(PasswordArg),
          context.arg(IsAuthenticatedArg),
          context.arg(OrdersArg).map(_.toList)
        )
        repo.saveUser(user)
      }
    )
   )
  )

  val UserSchema = Schema(QueryType, Some(MutationType))
}
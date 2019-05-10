import models._
import play.api.libs.json.Json
import repository.UserRepo
import sangria.schema._
import sangria.marshalling.playJson._
import sangria.macros.derive.{InputObjectTypeName, deriveInputObjectType, deriveObjectType}
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

  implicit val UserProducerType = deriveObjectType[Unit, UserProducer]()
  implicit val userProducerFormat = Json.format[UserProducer]
  implicit val UserProducerInputType = deriveInputObjectType[UserProducer](InputObjectTypeName("UserProducerFieldsInput"))

  implicit val UserCyclistType = deriveObjectType[Unit, UserCyclist]()
  implicit val userCyclistFormat  = Json.format[UserCyclist]
  implicit val UserCyclistInputType = deriveInputObjectType[UserCyclist](InputObjectTypeName("UserCyclistFieldsInput"))

  implicit val UserType = deriveObjectType[Unit, UserDomain]()
  implicit val userFormat = Json.format[UserDomain]
  implicit val UserInputType = deriveInputObjectType[UserDomain](InputObjectTypeName("UserFieldsInput"))

  implicit val PersonType: InterfaceType[Unit, Person] = InterfaceType("Person", "person description",
    () => fields[Unit, Person](
      Field("firstName", StringType, resolve = _.value.firstName),
      Field("lastName", StringType, resolve = _.value.lastName),
      Field("dni", StringType, resolve = _.value.dni)
    ))

  /* custom types*/


  /* Arguments*/
  val IdArg = Argument("id", OptionInputType(StringType))
  val DeviceArg = Argument("device", OptionInputType(DeviceInputType))
  val UserCyclistArg = Argument("userCyclist", OptionInputType(UserCyclistInputType))
  val UserProducerArg = Argument("userProducer", OptionInputType(UserProducerInputType))
  val EmailArg = Argument("email", OptionInputType(StringType))
  val PasswordArg = Argument("password", OptionInputType(StringType))
  val IsAuthenticatedArg = Argument("isAuthenticated", OptionInputType(BooleanType))
  val OrdersArg = Argument("orders", OptionInputType(ListInputType(OrderInputType)))

  val QueryType = ObjectType("Query", fields[UserRepo, Unit](
    Field("allUsers", ListType(UserType),
      description = Some("Returns a list of all available users."),
      resolve = _.ctx.allUsers
    )
   )
  )

  val arguments = List(
    IdArg,
    DeviceArg,
    UserCyclistArg,
    UserProducerArg,
    EmailArg,
    PasswordArg,
    IsAuthenticatedArg,
    OrdersArg
  )

  def buildUserDomain(context:Context[UserRepo, Unit]): UserDomain = {
    UserDomain(
      context.arg(IdArg),
      context.arg(DeviceArg),
      context.arg(UserCyclistArg),
      context.arg(UserProducerArg),
      context.arg(EmailArg),
      context.arg(PasswordArg),
      context.arg(IsAuthenticatedArg),
      context.arg(OrdersArg).map(_.toList)
    )
  }


  val MutationType = ObjectType("Mutation", fields[UserRepo, Unit](
    Field("addUser", UserType,
      arguments = arguments,
      resolve = context => {
        val repo = context.ctx
        repo.saveUser(buildUserDomain(context))
      }
    ),
    Field("updateEmail",StringType,
      arguments = arguments,
      resolve = context => {
        val repo = context.ctx
        repo.updateEmail(buildUserDomain(context))
      }
    ),
    Field("updatePassword",StringType,
      arguments = arguments,
      resolve = context => {
        val repo = context.ctx
        repo.updatePassword(buildUserDomain(context))
      }
    ),
    Field("updateUserType", StringType,
      arguments = arguments,
      resolve = context => {
        val repo = context.ctx
        repo.updateUserType(buildUserDomain(context))
      }
    )
   )
  )

  val UserSchema = Schema(QueryType, Some(MutationType))
}
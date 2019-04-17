import models._
import sangria.schema._

object SchemaDefinition {

  implicit val ProductType = ObjectType("Product", "product description",
    fields[Unit, Product](
      Field("name", StringType, resolve = _.value.name),
      Field("description", StringType, resolve = _.value.description),
      Field("photo", OptionType(StringType), resolve = _.value.photo),
      Field("isEspecial", OptionType(BooleanType), resolve = _.value.isSpecial)
    ))

  implicit val CoordinateType = ObjectType("Coordinate", "coordinate description",
    fields[Unit, Coordinate](
      Field("latitude", FloatType, resolve = _.value.latitude),
      Field("longitude", FloatType, resolve = _.value.longitude)
    ))

  implicit val GoalCanceledType = ObjectType("GoalCanceled", "goalCanceled description",
    fields[Unit, GoalCanceled](
      Field("time", OptionType(StringType), resolve = _.value.time),
      Field("reason", OptionType(StringType), resolve = _.value.reason)
    ))

  implicit val GoalType = ObjectType("Goal", "goal description",
    fields[Unit, Goal](
      Field("products", ListType(ProductType), resolve = _.value.products),
      Field("userCyclistId", OptionType(StringType), resolve = _.value.userCyclistId),
      Field("goalCoordinate", OptionType(CoordinateType), resolve = _.value.goalCoordinate),
      Field("goalTypeName", OptionType(StringType), resolve = _.value.goalTypeName),
      Field("goalCanceled", OptionType(GoalCanceledType), resolve = _.value.goalCanceled)
    ))

  implicit val OrderType = ObjectType("Order", "order description",
    fields[Unit, Order](
      Field("orderTypeName", OptionType(StringType), resolve = _.value.orderTypeName),
      Field("statusOrderTypeName", OptionType(StringType), resolve = _.value.statusOrderTypeName),
      Field("kilometers", OptionType(StringType), resolve = _.value.kilometers),
      Field("finalPrice", OptionType(StringType), resolve = _.value.finalPrice),
      Field("isPaid", OptionType(BooleanType), resolve = _.value.isPaid),
      Field("paymentDateTime", OptionType(StringType), , resolve = _.value.paymentDateTime),
      Field("paymentMethod", OptionType(StringType), resolve = _.value.paymentMethod),
      Field("goals",OptionType(ListType(GoalType)), resolve = _.value.goals)
    ))

}
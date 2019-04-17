package models

import org.joda.time.DateTime

//TODO: put Order class here ...
case class Goal(product: List[Product],
                cyclistId: Option[String],
                goalCoordinate: Coordinate,
                goalType: Option[_ <: GoalType],
                goalCanceled: Option[GoalCanceled])

case class Product(name:String,
                   description: String,
                   photo: Option[String],
                   isSpecial: Boolean)

case class GoalCanceled(time: Option[DateTime],
                        reason: Option[String])

sealed trait GoalType {
  val description: String
}

case object PICKUP extends GoalType {
  override val description: String = "week, months of the year"
}

case object LEAVE extends GoalType {
  override val description: String = "week, months of the year"
}

case object BUY extends GoalType {
  override val description: String = "week, months of the year"
}

case object PAY extends GoalType {
  override val description: String = "week, months of the year"
}

case object GoalTypeList {
  def getGoalTypeList: List[_ <: GoalType] = List(PICKUP, LEAVE, BUY, PAY)
}
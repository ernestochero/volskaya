package models

import org.joda.time.DateTime

case class Goal(products: Option[List[Product]],
                userCyclistId: Option[String],
                goalCoordinate: Option[Coordinate],
                goalTypeName: Option[String],
                goalCanceled: Option[GoalCanceled])

case class Product(name:String,
                   description: String,
                   photo: Option[String],
                   isSpecial: Option[Boolean])

//TODO: change to DateTime on time in the future
case class GoalCanceled(time: Option[String],
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
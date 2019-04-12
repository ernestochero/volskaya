package model

//TODO: put Order class here ...
case class Goal(product: Product)

case class Product(name:String,
                   description: String,
                   photo: Option[String],
                   isSpecial: Boolean)

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
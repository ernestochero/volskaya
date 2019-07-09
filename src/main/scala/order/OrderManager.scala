package user

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import models.Order
import models.OrderManagementMessages.SaveOrder
import org.mongodb.scala.MongoCollection
import akka.pattern.ask
import models.UserManagementExceptions._
import models.VolskayaMessages.VolskayaSuccessResponse
import order.OrderStorageActor
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import models.VolskayaMessages._
case class OrderManagerAPI(system: ActorSystem) {
  def orderManagementActor = system.actorSelection("/user/orderManagementActor")
  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  val log = system.log

  //test
  def storeOrder(order: Order) = {
    (orderManagementActor ? SaveOrder(order)).flatMap {
      case order: Order =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessSave(models.OrderFieldId)))
      case _ =>
        Future.failed(MatchPatternNotFoundException(getMatchPatternNotFoundMessage))
    }.recoverWith {
      case ex =>
        Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(ex.getMessage)))
    }
  }

}


class OrderManager(collection: MongoCollection[Order]) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))
  val orderStorageActor : ActorRef = context.watch(context.actorOf(Props(classOf[OrderStorageActor], collection), "orderStorage"))
  override def receive: Receive = {
    case msg@SaveOrder(order) =>
      orderStorageActor forward msg
  }

}

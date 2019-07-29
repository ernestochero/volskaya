package user

import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.util.Timeout
import models.{ Order, OrderDomain }
import models.OrderManagementMessages.{ GetAllOrders, SaveOrder }
import org.mongodb.scala.MongoCollection
import akka.pattern.ask
import order.OrderStorageActor

import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class OrderManagerAPI(system: ActorSystem) {
  def orderManagementActor = system.actorSelection("/user/orderManagementActor")
  import system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  val log = system.log

  def getAllOrders(limit: Int, offset: Int): Future[Seq[Order]] =
    (orderManagementActor ? GetAllOrders(limit, offset)).mapTo[Seq[Order]]

  def saveOrder(order: Order): Future[Order] =
    (orderManagementActor ? SaveOrder(order)).mapTo[Order]
}

class OrderManager(collection: MongoCollection[Order]) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))
  val orderStorageActor: ActorRef =
    context.watch(context.actorOf(Props(classOf[OrderStorageActor], collection), "orderStorage"))
  override def receive: Receive = {
    case msg @ GetAllOrders(limit, offset) =>
      orderStorageActor forward msg
    case msg @ SaveOrder(order) =>
      orderStorageActor forward msg
  }

}

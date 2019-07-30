package order

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import models.Order
import models.OrderManagementMessages._
import org.mongodb.scala.{ Document, MongoCollection }
import akka.pattern.pipe

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, SECONDS }

class OrderStorageActor(orderCollection: MongoCollection[Order]) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout              = Timeout(Duration.create(30, SECONDS))

  override def receive: Receive = {
    case SaveOrder(order) =>
      val saveOrderResult = orderCollection
        .insertOne(order)
        .toFuture()
        .recoverWith { case e => Future.failed(e) }
        .map(_ => order)
      saveOrderResult.pipeTo(sender())

    case GetAllOrders(limit, offset) =>
      val result = orderCollection.find().skip(offset).limit(limit).toFuture()
      result.pipeTo(sender())

    case GetOrder(id) =>
      val getOrderResult = orderCollection
        .find(Document("_id" -> id))
        .toFuture()
        .recoverWith { case e => Future.failed(e) }
        .map(_.headOption)
      getOrderResult.pipeTo(sender())

  }
}

package order

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import models.Order
import models.OrderManagementMessages.SaveOrder
import org.mongodb.scala.MongoCollection
import akka.pattern.pipe
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class OrderStorageActor(collection: MongoCollection[Order]) extends Actor with ActorLogging{
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout = Timeout(Duration.create(30, TimeUnit.SECONDS))

  override def receive: Receive = {
    case SaveOrder(order) => {
      val result =  collection.insertOne(order)
        .toFuture()
        .recoverWith { case e => Future.failed(e) }
        .map(_ => order)
      result.pipeTo(sender())
    }
  }
}

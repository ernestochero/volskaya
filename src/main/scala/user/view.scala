package user

import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnNext}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import models.Event

class UserView extends ActorSubscriber with akka.actor.ActorLogging {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy
  def receive = {
    case OnNext(event: Event) =>
      log.debug("subscribed {}", event.id)
    case OnComplete =>
      log.debug("finish process!")
  }
}
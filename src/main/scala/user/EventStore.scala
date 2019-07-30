package user

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import models.Event


class EventStore extends ActorPublisher[Event] {
  import MemoryEventStore._
  // in-memory event storage
  var events = Vector.empty[Event]
  var eventBuffer = Vector.empty[Event]

  def receive = {
    case AddEvent(event) if eventBuffer.size >= MaxBufferCapacity  =>
      sender() ! OverCapacity(event)
    case AddEvent(event) =>
      val entityEvents = events.filter(_.id == event.id)
      if (entityEvents.isEmpty) {
        addEvent(event)
        sender() ! EventAdded(event)
      } else {
        addEvent(event)
        sender() ! EventAlreadyExist(event)
      }
    case Request(_) => deliverEvents()
    case Cancel => context.stop(self)
  }

  def addEvent(event: Event) = {
    events  = events :+ event
    eventBuffer  = eventBuffer :+ event

    deliverEvents()
  }

  def deliverEvents(): Unit = {
    if (isActive && totalDemand > 0) {
      val (use, keep) = eventBuffer.splitAt(totalDemand.toInt)

      eventBuffer = keep

      use foreach onNext
    }
  }
}

object MemoryEventStore {
  case class AddEvent(event: Event)
  case class EventAdded(event: Event)
  case class OverCapacity(event: Event)
  case class EventAlreadyExist(event: Event)
  val MaxBufferCapacity = 1000
}
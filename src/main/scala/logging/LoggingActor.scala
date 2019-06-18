package logging

import akka.actor.{Actor, ActorLogging}
import logging.LoggingAction._

object LoggingAction {

  sealed trait LoggingMessage

  case class LogInfo(msg: String) extends LoggingMessage {}
  case class LogError(msg: String, ex: Throwable) extends  LoggingMessage {}
  case class LogDebug(msg: String) extends LoggingMessage {}
  case class LogWarning(msg: String) extends LoggingMessage {}

}

class LoggingActor extends Actor with ActorLogging {
  log.debug("initializing volskaya logging actor")

  override def receive: Receive = {
    case LogError(msg,ex) =>
      log.error(msg, ex)

    case LogWarning(msg) =>
      log.warning(msg)

    case LogInfo(msg) =>
      log.info(msg)

    case LogDebug(msg) =>
      log.debug(msg)
  }
}

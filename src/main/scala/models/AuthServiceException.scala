package models

object AuthServiceException {
  case class MissingToken() extends Throwable
  case class InvalidToken() extends Throwable
}

package fsmexample

import akka.actor.{Actor, ActorRef, FSM}
import akka.event.LoggingReceive
import fsmexample.BankAccount.{Deposit, Done, Failed, Withdraw}
import fsmexample.WireTransfer.Transfer

object BankAccount {

  case class Deposit(amount: BigInt) {
    require(amount > 0)
  }

  case class Withdraw(amount: BigInt) {
    require(amount > 0)
  }

  case object Done
  case object Failed

}

class BankAccount extends Actor {
  val balance = BigInt(0)


  override def receive: Receive = LoggingReceive {

    case Deposit(amount) =>
      balance += amount
      sender ! Done

    case Withdraw(amount) =>
      balance -= amount
      sender ! Done

    case _ => sender ! Failed
  }
}

object WireTransfer {
  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)

  case object Done

  case object Failed
}

class WireTransfer extends Actor {
  override def receive: Receive = LoggingReceive {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context.become(awaitFrom(to, amount, sender))
  }

  def awaitFrom(to: ActorRef, amount:BigInt, customer: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context.become(awaitTo(customer))

  }

  def awaitTo(customer: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      customer ! Done
      context.stop(self)
  }

}

sealed trait State
object Initial extends State
object AwaitFrom extends State
object AwaitTo extends State
object Done extends State

sealed trait Data
case object UninitializedWireTransferData extends Data
case class InitialisedWireTransferData(from: ActorRef, to: ActorRef, amount: BigInt, client: ActorRef) extends Data


class WireTransfer extends FSM[State, Data] {
  startWith(Initial, UninitializedWireTransferData)

  when(Initial) {
    case Event(Transfer(from, to, amount), UninitializedWireTransferData) =>
      from ! BankAccount.Withdraw(amount)
      goto(AwaitFrom) using InitialisedWireTransferData(from, to, amount, sender())
  }
  when(AwaitFrom) {
    case Event(BankAccount.Done, InitialisedWireTransferData(_, to, amount, _)) =>
      to ! BankAccount.Deposit(amount)
      goto(AwaitTo)
    case Event(BankAccount.Failed, InitialisedWireTransferData(_, _, _, client)) =>
      client ! WireTransfer.Failed
      goto(Done)
  }

  when(AwaitTo) {
    case Event(BankAccount.Done, InitialisedWireTransferData(_, _, _, client)) =>
      client ! BankAccount.Done
      goto(Done)
    case Event(BankAccount.Failed, InitialisedWireTransferData(_, _, _, client)) =>
      client ! WireTransfer.Failed
      goto(Done)
  }

  initialize()

}
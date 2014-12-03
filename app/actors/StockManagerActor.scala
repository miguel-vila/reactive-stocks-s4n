package actors

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import actors.StockManagerActor._
import play.libs.Akka

class StockManagerActor extends Actor with ActorLogging {

  import StockProtocol._

  log.info(s"### StockManagerActor created on ${self.path}")

  def getOrCreateStockActor(symbol: String): ActorRef = {
    context.child(symbol).getOrElse {
      context.actorOf(StockActor.props(symbol), symbol)
    }
  }

  def receive = {
    case watchStock @ WatchStock(symbol) =>
      getOrCreateStockActor(symbol) forward watchStock
    case unwatchStock @ UnwatchStock(symbol) =>
      context.child(symbol).foreach(_.forward(unwatchStock))
    case GetStockActorRefs =>
      sender() ! context.children
  }
}

object StockManagerActor {
  def props(): Props = Props(new StockManagerActor())
}
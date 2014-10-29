package actors

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import actors.StockManagerActor._
import play.libs.Akka

class StockManagerActor extends Actor with ActorLogging {

  import StockProtocol._

  log.info(s"### StockManagerActor created on ${self.path}")

  def receive = {
    case watchStock @ WatchStock(symbol) =>
      // get or create the StockActor for the symbol and forward this message
      context.child(symbol).getOrElse {
        context.actorOf(StockActor.props(symbol), symbol)
      } forward watchStock
    case unwatchStock @ UnwatchStock(symbol) =>
      // if there is a StockActor for the symbol forward this message
      context.child(symbol).foreach(_.forward(unwatchStock))
    // case unwatchStock @ UnwatchStock("None") =>
    //   // if no symbol is specified, forward to everyone
    //   context.children.foreach(_.forward(unwatchStock))
  }
}

object StockManagerActor {
  def props(): Props = Props(new StockManagerActor())
}

package actors

import akka.actor.ActorRef

object StockProtocol {

  // Commands
  sealed trait Cmd {
    def symbol: String
  }
  case class WatchStock(symbol: String) extends Cmd
  case class UnwatchStock(symbol: String) extends Cmd
  case class StockUpdate(symbol: String, price: Number) extends Cmd
  case class StockHistory(symbol: String, history: List[Double]) extends Cmd
  case class FetchLatest(symbol: String) extends Cmd
  case class Snap(symbol: String) extends Cmd
}

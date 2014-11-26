package actors

import akka.actor.ActorRef

object AverageStockProtocol {

  case class AddWatcherRef(watcher: ActorRef)
  case class ListenStock(symbol: String)
  case object ComputeAverageStock
  case class StockAverage(average: Double)

}

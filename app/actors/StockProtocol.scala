package actors

object StockProtocol {

  case class WatchStock(symbol: String)
  case class UnwatchStock(symbol: String)
  case class StockUpdate(symbol: String, price: Number)
  case class StockHistory(symbol: String, history: List[Double])
  case class FetchLatest(symbol: String)
  case object GetStockAverage
  case object GetStockActorRefs

}

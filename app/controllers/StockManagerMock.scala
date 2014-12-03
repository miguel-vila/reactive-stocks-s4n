package controllers

import model.{StockAverage, Stock}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object StockManagerMock {

  val stocks: List[Stock] = List(Stock("AAPL"),Stock("GOOGL"),Stock("ORCL"))

  def getStocksAverage(): Future[List[StockAverage]] = {
    val futures = stocks.map(stock => stock.getAverageStock())
    Future.sequence( futures )
  }
}
package backend

import model.{StockAverage, Stock}
import play.Play
import scala.collection.JavaConverters._

object AverageManagerMock {

  /**
  val defaultStocks = Play.application.configuration.getStringList("default.stocks")
  val stocks: List[Stock] = defaultStocks.asScala.map(s => Stock(s)).toList
    **/
  val stocks: List[Stock] = List(Stock("AAPL"),Stock("GOOGL"),Stock("ORCL"))

  def getStocksAverage(): List[StockAverage] = stocks.map(stock => stock.getAverageStock())
}

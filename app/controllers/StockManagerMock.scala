package controllers

import model.{StockAverage, Stock}

object StockManagerMock {

  val stocks: List[Stock] = List(Stock("AAPL"),Stock("GOOGL"),Stock("ORCL"))

  def getStocksAverage(): List[StockAverage] = {
    stocks.map(stock => stock.getAverageStock())
  }
}
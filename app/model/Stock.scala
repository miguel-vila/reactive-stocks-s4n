package model

import scala.util.Random

case class Stock(symbol: String) {

  val rand = Random

  def getAverageStock(): StockAverage = {
    Thread.sleep(1000)
    StockAverage(symbol, rand.nextDouble()*800)
  }
}

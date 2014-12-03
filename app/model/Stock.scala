package model

import scala.util.Random

case class Stock(symbol: String) {

  val rand = Random

  /**
   * Retorna el valor promedio de este Stock
   */
  def getAverageStock(): StockAverage = {
    ???
  }
}

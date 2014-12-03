package model

import scala.util.Random
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

case class Stock(symbol: String) {

  val rand = Random

  /**
   * Retorna el valor promedio de este Stock
   */
  def getAverageStock(): Future[StockAverage] = Future {
    Thread.sleep(1000)
    StockAverage(symbol, rand.nextDouble()*800)
  }
}

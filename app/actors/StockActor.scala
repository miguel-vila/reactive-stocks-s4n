package actors

import utils.FakeStockQuote
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.StockManagerActor._
import actors.StockActor._
import scala.util.Random

import akka.actor._
import akka.actor.ActorIdentity
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.actor.Identify
import akka.pattern.ask
import akka.persistence.{SnapshotOffer, PersistentActor}
import akka.util.Timeout

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends Actor with ActorLogging {

  import StockProtocol._
  import StockActor._

  implicit val identifyAskTimeout: Timeout = Duration(10, SECONDS)

  log.info(s"### StockActor creating StockActor for $symbol")

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val rand = Random

  // A random data set which uses stockQuote.newPrice to get each data point
  var stockHistory: Queue[Double] = {
    lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
    initialPrices.take(50).to[Queue]
  }

    // Fetch the latest stock value every 75ms
  val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

  override def receive: Receive = {
    case FetchLatest =>
      // add a new stock price to the history and drop the oldest
      val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
      stockHistory = stockHistory.drop(1) :+ newPrice
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, newPrice))
    case WatchStock(_) =>
      val watcher = sender()
      log.info(s"##StockActor adding watch stock for: $watcher")
      // send the stock history to the user
      watcher ! StockHistory(symbol, stockHistory.toList)
      // add the watcher to the list
      watchers = watchers + watcher
    case UnwatchStock(_) =>
      val watcher = sender()
      watchers = watchers - watcher
      if (watchers.size == 0) {
        stockTick.cancel()
        context.stop(self)
      }
  }
}

object StockActor {

  import StockProtocol._

  def props(symbol: String): Props = Props(new StockActor(symbol))

}

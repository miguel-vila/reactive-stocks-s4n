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
import akka.contrib.pattern.ShardRegion
import akka.contrib.pattern.ShardRegion.IdExtractor
import akka.contrib.pattern.ShardRegion.Passivate
import akka.pattern.ask
import akka.persistence.{SnapshotOffer, PersistentActor}
import akka.util.Timeout

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends PersistentActor with ActorLogging {

  import StockProtocol._
  import StockActor._

  // passivate the aggregate when no activity
  context.setReceiveTimeout(2.minutes)

  implicit val identifyAskTimeout: Timeout = Duration(10, SECONDS)

  log.info(s"### StockActor creating StockActor for $symbol")

  def persistenceId: String = return "symbol_" + symbol

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val rand = Random

  // A random data set which uses stockQuote.newPrice to get each data point
  var stockHistory: Queue[Double] = {
    lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
    initialPrices.take(50).to[Queue]
  }

    // Fetch the latest stock value every 75ms
  val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

  val snapshotTick = context.system.scheduler.schedule(Duration.Zero, 30.seconds, self, Snap(symbol))

  override def receiveCommand: Receive = {
    case FetchLatest =>
      // add a new stock price to the history and drop the oldest
      val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
      stockHistory = stockHistory.drop(1) :+ newPrice
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, newPrice))
    case WatchStock(_) =>
      persist(EventWatcherAdded(sender())) { eventWatcherAdded =>
        log.info(s"##StockActor adding watch stock for: ${eventWatcherAdded.watcher}")
        // send the stock history to the user
        eventWatcherAdded.watcher ! StockHistory(symbol, stockHistory.toList)
        // add the watcher to the list
        watchers = watchers + eventWatcherAdded.watcher
      }
    case UnwatchStock(_) =>
      persist(EventWatcherRemover(sender())) {  eventWatcherRemoved =>
        watchers = watchers - eventWatcherRemoved.watcher
        if (watchers.size == 0) {
          stockTick.cancel()
          context.stop(self)
        }
      }
    case AddWatcherAfterRecover(watcher) => 
      watchers = watchers + watcher
    case Snap(_) => {
      log.info(s"stock history size: ${stockHistory.size}")
      saveSnapshot(TakeSnapshot(stockHistory, watchers))
    }
  }


  override def receiveRecover: Receive = {
    case EventWatcherAdded(watcher) => addWatcherIfAlive(watcher)
    case EventWatcherRemover(watcher) => watchers = watchers - watcher
    case SnapshotOffer(_, takeSnapshot:TakeSnapshot) => {
      stockHistory = takeSnapshot.stockHistory
      log.info(s"recovering stockHistory size: ${stockHistory.size}")
      takeSnapshot.watchers.foreach(watcher => addWatcherIfAlive(watcher))
    }
  }

  private def addWatcherIfAlive(watcher: ActorRef) {
    (watcher ? Identify(watcher.path.name)).mapTo[ActorIdentity].map { actorIdentity =>
      actorIdentity.ref match {
        case Some(watcher) => self ! AddWatcherAfterRecover(watcher)
        case None => self ! UnwatchStock(symbol)
      }
    }.recover {
      case failure =>
      self ! UnwatchStock(symbol)
    }
  }
}

object StockActor {

  import StockProtocol._

  def props(symbol: String): Props = Props(new StockActor(symbol))

  case class TakeSnapshot(stockHistory: Queue[Double], watchers: HashSet[ActorRef])
  case class AddWatcherAfterRecover(watcher: ActorRef)

  /** Interface of the partial function used by the [[ShardRegion]] to
    * extract the entry id and the message to send to the entry from an
    * incoming message.
    */
  val idExtractor: ShardRegion.IdExtractor = {
    case c: Cmd => (c.symbol, c)
  }

  /** Interface of the function used by the [[ShardRegion]] to
    * extract the shard id from an incoming message.
    * Only messages that passed the [[IdExtractor]] will be used
    * as input to this function.
    */
  val shardResolver: ShardRegion.ShardResolver = {
    case c: Cmd => (math.abs(c.symbol.hashCode) % 100).toString
  }

  /** Shard name to manage [[User]] aggregates. */
  val aggName: String = "StockActor"
}

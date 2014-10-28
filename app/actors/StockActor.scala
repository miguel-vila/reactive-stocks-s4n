package actors

import akka.actor._
import utils.FakeStockQuote
import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.StockManagerActor._
import actors.StockActor._
import scala.util.Random
import akka.persistence.{SnapshotOffer, PersistentActor}

import akka.pattern.ask
import akka.util.Timeout
import actors.StockManagerActor.UnwatchStock
import actors.StockManagerActor.WatchStock
import actors.StockManagerActor.StockUpdate
import akka.actor.ActorIdentity
import akka.actor.Identify
import actors.StockManagerActor.StockHistory
import actors.StockManagerActor.UnwatchStock
import actors.StockActor.AddWatcherAfterRecover
import actors.StockManagerActor.WatchStock
import actors.StockManagerActor.StockUpdate
import akka.actor.ActorIdentity
import akka.actor.Identify
import actors.StockManagerActor.StockHistory

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends PersistentActor with ActorLogging {

    implicit val identifyAskTimeout: Timeout = Duration(10, SECONDS)

    log.info(s"### StockActor creating StockActor for $symbol")

    def persistenceId: String = {
        return "symbol_" + symbol
    }

    protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

    val rand = Random

    // A random data set which uses stockQuote.newPrice to get each data point
    var stockHistory: Queue[Double] = {
        lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
        initialPrices.take(50).to[Queue]
    }

    // Fetch the latest stock value every 75ms
    val stockTick = context.system.scheduler.schedule(Duration.Zero, 75.millis, self, FetchLatest)

    val snapshotTick = context.system.scheduler.schedule(Duration.Zero, 30.seconds, self, Snap)

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
        case AddWatcherAfterRecover(watcher) => watchers = watchers + watcher

        case Snap => {
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
        (watcher ? Identify(watcher.path.name)).mapTo[ActorIdentity].map {
            actorIdentity =>
                actorIdentity.ref match {
                    case Some(watcher) => self ! AddWatcherAfterRecover(watcher)
                    case None => self ! UnwatchStock(Option(symbol))
                }

        }.recover {
            case failure =>
                self ! UnwatchStock(Option(symbol))
        }
    }
}

object StockActor {

    def props(symbol: String): Props =
        Props(new StockActor(symbol))

    case object FetchLatest

    case class EventStockPriceUpdated(price: Double)

    case class EventWatcherAdded(watcher: ActorRef)

    case class EventWatcherRemover(watcher: ActorRef)

    case class TakeSnapshot(stockHistory: Queue[Double], watchers: HashSet[ActorRef])

    case class AddWatcherAfterRecover(watcher: ActorRef)

    case object Snap


}
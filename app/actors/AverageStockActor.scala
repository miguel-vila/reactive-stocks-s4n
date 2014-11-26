package actors

import actors.AverageStockProtocol.{StockAverage, ComputeAverageStock, AddWatcherRef}
import actors.StockProtocol._
import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import scala.collection.immutable.HashSet
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by miguel on 26/11/14.
 */
class AverageStockActor(defaultWatcher: ActorRef) extends Actor with ActorLogging with ActorManagerActor {

  import context._

  val stockManagerActor = actorManager.stockManagerActor
  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]
  watchers += defaultWatcher
  protected[this] var stockActors: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val stockAvgTick = context.system.scheduler.schedule(5 seconds, 2 seconds, self, ComputeAverageStock)
  implicit val timeout = Timeout(5 seconds)

  def receive: Receive = {
    case StockActorRef(stockActorRef) =>
      stockActors += stockActorRef
    case AddWatcherRef(watcher) =>
      watchers += watcher
    case ComputeAverageStock =>
      if(stockActors.size>0) {
        val stockUpdatesFutures = stockActors.toList.map(stockActor => (stockActor ? FetchLatestAndAnswerToSender).mapTo[StockUpdate])
        val futureStockUpdates = Future.sequence(stockUpdatesFutures)
        val futureAverage = futureStockUpdates.map( stockUpdates => average(stockUpdates) )
        watchers.foreach( watcher => futureAverage pipeTo watcher )
      }
  }

  def average(stockUpdates: List[StockUpdate]): StockAverage = {
    val sum = stockUpdates.foldLeft(0.0){ (acc, stockUpdate) => acc + stockUpdate.price.doubleValue() }
    StockAverage( sum / stockUpdates.length )
  }

}

object AverageStockActor {
  def props(defaultWatcher: ActorRef) = Props(new AverageStockActor(defaultWatcher))
}
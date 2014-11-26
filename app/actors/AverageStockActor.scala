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
class AverageStockActor extends Actor with ActorLogging with ActorManagerActor {

  import context._

  val stockManagerActor = actorManager.stockManagerActor
  protected[this] var stockActors: HashSet[ActorRef] = HashSet.empty[ActorRef]

  implicit val timeout = Timeout(5 seconds)

  def receive: Receive = {
    case StockActorRef(stockActorRef) =>
      stockActors += stockActorRef
    case ComputeAverageStock =>
      if(stockActors.size>0) {
        val originalSender = sender()
        val stockUpdatesFutures = stockActors.toList.map(stockActor => (stockActor ? FetchLatestAndAnswerToSender).mapTo[StockUpdate])
        val futureStockUpdates = Future.sequence(stockUpdatesFutures)
        val futureAverage = futureStockUpdates.map( stockUpdates => average(stockUpdates) )
        futureAverage pipeTo originalSender
      }
  }

  def average(stockUpdates: List[StockUpdate]): StockAverage = {
    val sum = stockUpdates.foldLeft(0.0){ (acc, stockUpdate) => acc + stockUpdate.price.doubleValue() }
    StockAverage( sum / stockUpdates.length )
  }

}

object AverageStockActor {
  def props() = Props(new AverageStockActor())
}
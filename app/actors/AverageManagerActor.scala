package actors

import actors.StockProtocol.{GetStockActorRefs, GetStockAverage}
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import model.{StockAverage, Stock}
import akka.actor.{Props, ActorRef, Actor}
import akka.pattern.{ ask , pipe }
import scala.concurrent.duration._

class AverageManagerActor extends Actor with ActorManagerActor {

  var originalSender: ActorRef = _
  implicit val timeout = Timeout(5 seconds)

  def receive = {
    case GetStockAverage =>
      originalSender = sender()
      actorManager.stockManagerActor ! GetStockActorRefs
    case stockActorRefs: Iterable[ActorRef] =>
      val stockAverageFutures = stockActorRefs.map { stockActorRef =>
        (stockActorRef ? GetStockAverage).mapTo[StockAverage]
      }
      Future.sequence(stockAverageFutures) pipeTo originalSender
  }

}

object AverageManagerActor {
  def props = Props(new AverageManagerActor)
}

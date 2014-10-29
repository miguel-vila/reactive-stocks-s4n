package actors

import akka.actor._
import akka.cluster.Cluster
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.MemberUp
import scala.Some
// import actors.StockManagerActor.{UnwatchStock, WatchStock}


class StockManagerProxy extends Actor with ActorLogging  with Stash {

  import StockProtocol._

  Cluster(context.system).subscribe(self, classOf[MemberUp])

  def watchForStockManager: Receive = {
    case MemberUp(member) if member.hasRole("backend") => {
      val path = RootActorPath(member.address) / "user" / "stockManager"
      val stockManager = context actorSelection path
      unstashAll()
      context.become(ready(stockManager))
    }
    case _ => stash()
  }

  override def receive: Receive = watchForStockManager

  def ready(stockManager:ActorSelection): Receive = {
    case watchStock:WatchStock =>  stockManager forward watchStock
    case unwatchStock:UnwatchStock => stockManager forward unwatchStock
  }
}

object StockManagerProxy {
  def props(): Props = Props(new StockManagerProxy())
}

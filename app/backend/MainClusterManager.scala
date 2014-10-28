package backend

import akka.actor.{ActorSystem, PoisonPill}
import actors.{StockManagerActor, Settings}
import akka.contrib.pattern.ClusterSingletonManager

/**
 * Main class for starting cluster nodes.
 */
object MainClusterManager extends BaseApp {

    override protected def initialize(system: ActorSystem, settings: Settings): Unit = {
      // system.actorOf(StockManagerActor.props, "stockManager")
      system.actorOf(
        ClusterSingletonManager.props(
          StockManagerActor.props,
          "stockManger",
          PoisonPill,
          Some("backend")
        ),
      "singleton"
      )
    }
}

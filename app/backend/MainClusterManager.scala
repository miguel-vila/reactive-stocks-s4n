package backend

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.ClusterSharding
// import akka.contrib.pattern.ClusterSingletonManager

// import actors.{StockManagerActor, Settings}
import actors.{StockActor, Settings}

import backend.journal.SharedJournalSetter

/**
 * Main class for starting cluster nodes.
 */
object MainClusterManager extends BaseApp {

  override protected def initialize(system: ActorSystem, settings: Settings): Unit = {
    // system.actorOf(StockManagerActor.props, "stockManager")

    // System.out.println("###Starting Cluster Singleton Manager")

    // system.actorOf(
    //   ClusterSingletonManager.props(
    //     StockManagerActor.props,
    //     "stockManger",
    //     PoisonPill,
    //     Some("backend")
    //   ),
    //   "singleton"
    // )

    System.out.println("### Starting Shard Region")

    // Create Sharded region for user
    val StockRegion = ClusterSharding(system).start(
      typeName      = StockActor.aggName,
      // entryProps    = Some(Props[StockActor]),
      entryProps    = Some(StockActor.props("None")),
      idExtractor   = StockActor.idExtractor,
      shardResolver = StockActor.shardResolver)

    system.actorOf(SharedJournalSetter.props, "shared-journal-setter")
  }
}

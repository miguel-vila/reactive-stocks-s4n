package actors

import akka.actor._

import backend.SentimentActor
import akka.routing.FromConfig
import backend.journal.SharedJournalSetter
import akka.contrib.pattern.ClusterSingletonProxy

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

    // val stockManagerProxy = system.actorOf(StockManagerProxy.props)
    val stockManagerProxy = system.actorOf(
        ClusterSingletonProxy.props("/user/singleton/stockManger", Some("backend")),
        "stockManagerProxy")

    val sentimentActor = system.actorOf(FromConfig.props(SentimentActor.props), "sentimentRouter")

    system.actorOf(SharedJournalSetter.props, "shared-journal-setter")

}

trait ActorManagerActor {
    this: Actor =>

    val actorManager: ActorManager =
        ActorManager(context.system)
}

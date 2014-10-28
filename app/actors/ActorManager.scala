package actors

import akka.actor._
import scala.concurrent.duration.{MILLISECONDS => Millis}

import backend.SentimentActor
import akka.routing.FromConfig
import backend.journal.SharedJournalSetter
import akka.contrib.pattern.ClusterSingletonProxy

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

  // val stockManagerProxy = system.actorOf(StockManagerProxy.props)
  val stockManagerProxy = system.actorOf(
    ClusterSingletonProxy.props("/user/singleton/my-actor",Some("my-role")),
    "my-actor-proxy")
  val sentimentActor = system.actorOf(FromConfig.props(SentimentActor.props),"sentimentRouter")
}

trait ActorManagerActor {
    this: Actor =>

    val actorManager: ActorManager =
        ActorManager(context.system)
}

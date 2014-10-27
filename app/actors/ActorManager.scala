package actors

import akka.actor._
import scala.concurrent.duration.{MILLISECONDS => Millis}

import backend.SentimentActor
import akka.routing.FromConfig
import backend.journal.SharedJournalSetter

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

  val stockManagerProxy = system.actorOf(StockManagerProxy.props)
  val sentimentActor = system.actorOf(SentimentActor.props)
}

trait ActorManagerActor {
    this: Actor =>

    val actorManager: ActorManager =
        ActorManager(context.system)
}

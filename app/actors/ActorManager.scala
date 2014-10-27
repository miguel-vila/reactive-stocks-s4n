package actors

import akka.actor._
import scala.concurrent.duration.{MILLISECONDS => Millis}

import backend.SentimentActor
import akka.routing.FromConfig
import backend.journal.SharedJournalSetter

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

    val address = Address("akka.tcp", "application", "localhost", 2555)
    val path = RootActorPath(address) / "user" / "stockManager"
    val stockManager = system.actorSelection(path)

    val sentimentActor = system.actorOf(SentimentActor.props)
}

trait ActorManagerActor {
    this: Actor =>

    val actorManager: ActorManager =
        ActorManager(context.system)
}

package actors

import akka.actor._
import akka.routing.FromConfig

object ActorManager extends ExtensionKey[ActorManager]

class ActorManager(system: ExtendedActorSystem) extends Extension {

  val stockManagerActor = system.actorOf(StockManagerActor.props)

}

trait ActorManagerActor { this: Actor =>
  val actorManager: ActorManager = ActorManager(context.system)
}

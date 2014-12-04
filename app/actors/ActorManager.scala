package actors

import play.libs.Akka.system

object ActorManager {

  val stockManagerActor = system.actorOf(StockManagerActor.props)

}
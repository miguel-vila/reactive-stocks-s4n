package actors

import actors.AverageStockProtocol.{ListenStock, StockAverage}
import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.util.Timeout
import play.api.libs.json._
import play.Play
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
// import actors.StockManagerActor.StockHistory
import akka.pattern.ask
import akka.pattern.pipe
import scala.collection.JavaConverters._
import scala.concurrent.duration._

/** The out actor is wired in by Play Framework when this Actor is created.
  * When a message is sent to out the Play Framework then sends it to the client WebSocket.
  *
  * */
class UserActor(out: ActorRef) extends Actor with ActorLogging with ActorManagerActor {
  import context._
  import StockProtocol._

  val stockManagerActor = actorManager.stockManagerActor
  val averageStockActor = context.system.actorOf( AverageStockActor.props(self) )
  implicit val askTimeout = Timeout(5 seconds)

    // watch the default stocks
  val defaultStocks = Play.application.configuration.getStringList("default.stocks")
  for (stockSymbol <- defaultStocks.asScala) {
    stockManagerActor ! WatchStock(stockSymbol)
  }
  /* @TODO: Reemplazando lo anterior con lo siguiente deja de funcionar
  for (stockSymbol <- defaultStocks.asScala) {
  val futureStockRef = (stockManagerActor ? WatchStock(stockSymbol)).mapTo[StockActorRef]
  futureStockRef pipeTo averageStockActor
}
   */

  def receive = {
    //Handle the FetchTweets message to periodically fetch tweets if there is a query available.
    case StockUpdate(symbol, price) =>
      val stockUpdateJson: JsObject = JsObject(Seq(
        "type" -> JsString("stockupdate"),
        "symbol" -> JsString(symbol),
        "price" -> JsNumber(price.doubleValue())
      ))
      out ! stockUpdateJson

    case StockHistory(symbol, history) =>
      val jsonHistList = JsArray(history.map(price => JsNumber(price)))
      val stockHistoryJson: JsObject = JsObject(Seq(
        "type" -> JsString("stockhistory"),
        "symbol" -> JsString(symbol),
        "history" -> jsonHistList
      ))
      out ! stockHistoryJson

    case StockAverage(average) =>
      val stockAverageJson = JsObject(Seq(
        "type" -> JsString("stockaverage"),
        "average" -> JsNumber(average)
      ))
      out ! stockAverageJson

    case message: JsValue =>
      (message \ "symbol").asOpt[String] match {
        case Some(symbol) =>
          val futureStockRef = (stockManagerActor ? WatchStock(symbol)).mapTo[StockActorRef]
          futureStockRef pipeTo averageStockActor
        case None => log.error("symbol was not found in json: $message")
      }
      (message \ "message").asOpt[String] match {
        case Some("GET_AVG_STOCK") => 
          println("RECEIVED GET_AVG_STOCK!!!")
        case None => log.error("message was not found in json: $message")
      }
      
  }

  override def postStop() {
    stockManagerActor ! UnwatchStock("None")
  }
}

object UserActor {
  def props(out: ActorRef) = Props(new UserActor(out))
}

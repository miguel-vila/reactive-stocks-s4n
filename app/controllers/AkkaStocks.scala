package controllers

import actors.StockProtocol.GetStockAverage
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import model.StockAverage
import play.api.mvc.{AnyContent, WebSocket, Action, Controller}
import actors.{AverageManagerActor, UserActor}
import play.api.libs.json._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.libs.Akka
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Controlador de Play para manejar Stocks
 */
object AkkaStocks extends Controller {

  /**
   * Esto es para poder serializar objetos de tipo StockAverage a JSON
   */
  implicit val stockAverageFormat = Json.format[StockAverage]

  val avgStockActor: ActorRef = Akka.system.actorOf( AverageManagerActor.props )
  implicit val timeout = Timeout( 5 seconds )

  /**
   * Vista de index
   */
  def index = Action {
    Ok(views.html.index())
  }

  /**
   * Websocket
   */
  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { implicit request => out =>
    UserActor.props(out)
  }

  /**
   * Devuelve el promedio de stocks
   */
  def averageStocks(): Action[AnyContent] = Action.async { req =>
    val stockAvgsFuture: Future[Iterable[StockAverage]] = (avgStockActor ? GetStockAverage).mapTo[Iterable[StockAverage]]
    stockAvgsFuture.map(averages => Ok(Json.toJson(averages)))
  }

}

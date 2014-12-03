package controllers

import model.StockAverage
import play.api.mvc.{AnyContent, WebSocket, Action, Controller}
import actors.UserActor
import play.api.libs.json._
import play.api.Play.current

/**
 * Controlador de Play para manejar Stocks
 */
object AkkaStocks extends Controller {

  /**
   * Esto es para poder serializar objetos de tipo StockAverage a JSON
   */
  implicit val stockAverageFormat = Json.format[StockAverage]

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
  def averageStocks(): Action[AnyContent] = Action { req =>
    val averages = StockManagerMock.getStocksAverage()
    Ok(Json.toJson(averages))
  }

}

package controllers

import backend.AverageManagerMock
import model.StockAverage
import play.api.mvc.{AnyContent, WebSocket, Action, Controller}
import actors.UserActor
import play.api.libs.json._

import play.api.Play.current

object AkkaStocks extends Controller {

  implicit val stockAverageFormat = Json.format[StockAverage]

  def index = Action {
        Ok(views.html.index())
    }

    def ws = WebSocket.acceptWithActor[JsValue, JsValue] { implicit request => out =>
        UserActor.props(out)
    }

    def averageStocks(): Action[AnyContent] = Action { req =>
      val averages = AverageManagerMock.getStocksAverage()
      Ok(Json.toJson(averages))
    }

}

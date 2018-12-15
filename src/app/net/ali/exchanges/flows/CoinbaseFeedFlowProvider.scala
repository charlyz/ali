package net.ali.exchanges.flows

import javax.inject.Inject
import javax.inject.Singleton
import net.ali._
import net.ali.models._
import net.ali.json.SnakeJsonFormat._
import akka.actor.ActorSystem
import akka.Done
import play.api.libs.json.JsArray
import play.api.libs.json.JsError
import play.api.libs.json.JsLookupResult.jsLookupResultToJsLookup
import play.api.libs.json.JsSuccess
import play.api.libs.json._
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Reads
import play.api.libs.json._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import play.api.libs.json._
import akka.http.scaladsl.model.{DateTime => AkkaDateTime, _}
import org.joda.time._
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl.Flow
import scala.concurrent._
import net.ali.json.JodaDateFormat.jodaDateReadsForMicroSeconds
import net.ali.json.PriceTickReads
import com.google.inject.Provider
import play.api.libs.functional.syntax.toFunctionalBuilderOps

@Singleton
class CoinbaseFeedFlowProvider @Inject()(
  config: AliConfiguration,
  priceTickReads: PriceTickReads,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem
) extends Provider[Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]]] {
  
  implicit val priceTickReadsFromCoinbase = priceTickReads.priceTickReadsFromCoinbase
  
  @volatile var lastPriceTickOpt: Option[PriceTick] = None
  
  val webSocketFlow = Http()
    .webSocketClientFlow(WebSocketRequest(config.Coinbase.WebSocketURL))
    .map { 
      case TextMessage.Strict(payloadAsString) =>
        val payloadAsJson = Json.parse(payloadAsString)

        (payloadAsJson \ "type").asOpt[String] match {
          case Some("ticker") => 
            payloadAsJson.validate[PriceTick] match {
              case JsSuccess(priceTick, _) => 
                lastPriceTickOpt = Some(priceTick)
                lastPriceTickOpt
              case JsError(e) => throw new Exception(s"Payload could not be parsed: $e")
            }
          case Some("heartbeat") => 
             (payloadAsJson \ "time").asOpt[DateTime](jodaDateReadsForMicroSeconds) match {
               case Some(heartbeatTime) => 
                 lastPriceTickOpt match {
                   case Some(lastPriceTick) => 
                     lastPriceTickOpt = Some(lastPriceTick.copy(asOf = heartbeatTime))
                     lastPriceTickOpt
                   case _ => None
                 }
               case _ => throw new Exception(s"Heartbeat time could not be found: $payloadAsString")
             }
          case Some("subscriptions") => None
          case _ => throw new Exception(s"Unexpected event: $payloadAsString")
        }
      case _ => None
    }
    .collect {
      case Some(priceTick) => priceTick
    }
  
  override def get(): Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]] = webSocketFlow
  
}
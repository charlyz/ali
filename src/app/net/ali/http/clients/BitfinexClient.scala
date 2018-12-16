package net.ali.http.clients

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.TimeoutException
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import javax.crypto._
import javax.crypto.spec._
import net.ali._
import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.ali.http._
import org.joda.time._
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.JsObject
import org.apache.commons.codec.binary.Hex
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import java.util.Base64

@Singleton
class BitfinexClient @Inject()(
  config: AliConfiguration,
  ws: WSClient,
  implicit val actorSystem: ActorSystem,
  implicit val ec: ExecutionContext,
  implicit val mat: Materializer
) extends HttpClient {
  
  val mac = Mac.getInstance("HmacSHA384")
  mac.init(new SecretKeySpec(config.Bitfinex.SecretKey.getBytes, "HmacSHA384"))
  
  def createMarketBuyOrder() = {
    val body = s"""
      |{
      |  "request": "/v1/order/new",
      |  "nonce": "${DateTime.now.getMillis}",
      |  "symbol": "${config.LeftPair}${config.RightPair}",
      |  "amount": "${config.CoinsQuantity}",
      |  "price": "0.0000001",
      |  "exchange": "bitfinex",
      |  "side": "buy",
      |  "type": "market"
      |}
      """.stripMargin

    createOrder(Json.stringify(Json.parse(body)))
  }
  
  def createMarketSellOrder() = {
    val body = s"""
      |{
      |  "request": "/v1/order/new",
      |  "nonce": "${DateTime.now.getMillis}",
      |  "symbol": "${config.LeftPair}${config.RightPair}",
      |  "amount": "${config.CoinsQuantity}",
      |  "price": "100000000",
      |  "exchange": "bitfinex",
      |  "side": "sell",
      |  "type": "market"
      |}
      """.stripMargin

    createOrder(Json.stringify(Json.parse(body)))
  }
  
  def createOrder(body: String): Future[Unit] = {
    val path = "/v1/order/new"
    val url = s"https://${config.Bitfinex.ApiHostname}$path"
    val timestamp = DateTime.now.getMillis
    
    val payload = Base64.getEncoder.encodeToString(body.getBytes)

    Try {
      new String(Hex.encodeHex(mac.doFinal(payload.getBytes)))
    } match {
      case Success(signature) => 
        val headers = Seq(
          "X-BFX-APIKEY" -> config.Bitfinex.PublicKey,
          "X-BFX-PAYLOAD" -> payload,
          "X-BFX-SIGNATURE" -> signature,
          "content-type" -> "application/data"
        )

        val responseFuture = ws
          .url(url)
          .withRequestTimeout(config.Bitfinex.HttpRequestTimeout)
          .addHttpHeaders(headers: _*)
          .post(body)
    
        logSendingRequestInfo(url, "POST", Some(headers), paramsOpt = None, Some(body))
        val t0 = System.nanoTime()
    
        responseFuture.onSuccess {
          case response =>
            logReceivingResponseInfo(
              url,
              "POST",
              Some(headers),
              paramsOpt = None,
              t0,
              t1 = System.nanoTime(),
              response.status,
              Some(response.body.length),
              Some(response.body)
            )
        }
    
        responseFuture
          .map { response =>
            if (response.status != 200) {
              throw new Exception(s"Order could not be created: ${response.body}")
            } else {
              ()
            }
          }
      case Failure(e) => Future.failed(e)
    }
  }

}

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
class BinanceClient @Inject()(
  config: AliConfiguration,
  ws: WSClient,
  implicit val actorSystem: ActorSystem,
  implicit val ec: ExecutionContext,
  implicit val mat: Materializer
) extends HttpClient {
  
  val mac = Mac.getInstance("HmacSHA256")
  mac.init(new SecretKeySpec(config.Binance.SecretKey.getBytes, "HmacSHA256"))
  
  def createMarketBuyOrder() = {
    createOrder(
      side = "BUY",
      orderType = "MARKET"
    )
  }
  
  def createMarketSellOrder() = {
    createOrder(
      side = "SELL",
      orderType = "MARKET"
    )
  }
  
  def createOrder(side: String, orderType: String): Future[Unit] = {
    val path = "/api/v3/order/test"
    val url = s"https://${config.Binance.ApiHostname}$path"
    val timestamp = DateTime.now.getMillis
    
    val paramsWithoutSignature = Seq(
      "symbol" -> s"${config.LeftPair}${config.RightPair}",
      "side" -> side,
      "type" -> orderType,
      "quantity" -> config.CoinsQuantity.toString,
      "recvWindow" -> "5000",
      "timestamp" -> timestamp.toString
    )
    
    val paramsWithoutSignatureAsString = paramsWithoutSignature
      .map { case (paramName, value) =>
        s"$paramName=$value"
      }
      .mkString("&")

    Try {
      new String(Hex.encodeHex(mac.doFinal(paramsWithoutSignatureAsString.getBytes())))
    } match {
      case Success(signature) => 
        val headers = Seq(
          "X-MBX-APIKEY" -> config.Binance.PublicKey,
          "content-type" -> "application/data"
        )
        
        val body = s"$paramsWithoutSignatureAsString&signature=$signature"

        val responseFuture = ws
          .url(url)
          .withRequestTimeout(config.Binance.HttpRequestTimeout)
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

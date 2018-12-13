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
class CoinbaseClient @Inject()(
  config: AliConfiguration,
  ws: WSClient,
  implicit val actorSystem: ActorSystem,
  implicit val ec: ExecutionContext,
  implicit val mat: Materializer
) extends HttpClient {
  
  val decodedSecret = Base64.getDecoder.decode(config.Coinbase.SecretKey)
  val mac = Mac.getInstance("HmacSHA256")
  mac.init(new SecretKeySpec(decodedSecret, "HmacSHA256"))
  
  def createLimitBuyOrder(price: Double) = {
    val body = s"""
      | {
      |   "size": "${config.CoinSize}",
      |   "price": "$price",
      |   "side": "buy",
      |   "type": "limit",
      |   "post_only": true,
      |   "product_id": "${config.LeftPair}-${config.RightPair}"
      | }
      """.stripMargin

    createOrder(Json.stringify(Json.parse(body)))
  }
  
  def createMarketBuyOrder() = {
    val body = s"""
      | {
      |   "size": "${config.CoinSize}",
      |   "side": "buy",
      |   "type": "market",
      |   "product_id": "${config.LeftPair}-${config.RightPair}"
      | }
      """.stripMargin

    createOrder(Json.stringify(Json.parse(body)))
  }
  
  def createMarketSellOrder() = {
    val body = s"""
      | {
      |   "size": "${config.CoinSize}",
      |   "side": "sell",
      |   "type": "market",
      |   "product_id": "${config.LeftPair}-${config.RightPair}"
      | }
      """.stripMargin

    createOrder(Json.stringify(Json.parse(body)))
  }
  
  def createOrder(body: String): Future[Unit] = {
    val path = "/orders"
    val url = s"https://${config.Coinbase.ApiHostname}$path"
    val timestamp = DateTime.now.getMillis / 1000
    val message = timestamp + "POST" + path + body

    Try {
      Base64.getEncoder.encodeToString(mac.doFinal(message.getBytes()))
    } match {
      case Success(signature) => 
        val headers = Seq(
          "CB-ACCESS-KEY" -> config.Coinbase.PublicKey,
          "CB-ACCESS-SIGN" -> signature,
          "CB-ACCESS-TIMESTAMP" -> timestamp.toString,
          "CB-ACCESS-PASSPHRASE"  -> config.Coinbase.Passphrase,
          "Content-Type" -> "application/json"
        )
    
        val responseFuture = ws
          .url(url)
          .withRequestTimeout(config.Coinbase.HttpRequestTimeout)
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
  
  def getAccounts: Future[String] = {
    val path = "/accounts"
    val url = s"https://${config.Coinbase.ApiHostname}$path"
    val timestamp = DateTime.now.getMillis / 1000
    val message = timestamp + "GET" + path

    Try {
      Base64.getEncoder.encodeToString(mac.doFinal(message.getBytes()))
    } match {
      case Success(signature) => 
        val headers = Seq(
          "CB-ACCESS-KEY" -> config.Coinbase.PublicKey,
          "CB-ACCESS-SIGN" -> signature,
          "CB-ACCESS-TIMESTAMP" -> timestamp.toString,
          "CB-ACCESS-PASSPHRASE"  -> config.Coinbase.Passphrase
        )
    
        val responseFuture = ws
          .url(url)
          .withRequestTimeout(config.Coinbase.HttpRequestTimeout)
          .addHttpHeaders(headers: _*)
          .get
    
        logSendingRequestInfo(url, "GET", Some(headers), paramsOpt = None, bodyOpt = None)
        val t0 = System.nanoTime()
    
        responseFuture.onSuccess {
          case response =>
            logReceivingResponseInfo(
              url,
              "GET",
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
            println(response.body)
            response.body
          }
      case Failure(e) => Future.failed(e)
    }
  }

}

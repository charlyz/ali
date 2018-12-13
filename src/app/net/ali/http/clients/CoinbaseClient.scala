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

  def getAccounts: Future[String] = {
    val path = "/accounts"
    val url = s"http://${config.Coinbase.ApiHostname}$path"
    val timestamp = DateTime.now.getMillis / 1000
    val message = timestamp + "GET" + path
    /*
     * 
     * String prehash = timestamp + method.toUpperCase() + requestPath + body;
        byte[] secretDecoded = Base64.decode(secretKey, Base64.DEFAULT);

        SecretKeySpec keyspec = new SecretKeySpec(secretDecoded, "HmacSHA256");
        Mac sha256 = GDAXConstants.SHARED_MAC;
        sha256.init(keyspec);
        String shadone = Base64.encodeToString(sha256.doFinal(prehash.getBytes()),Base64.DEFAULT);
        return shadone;
     */
    Try {
      /*val mac = Mac.getInstance("HmacSHA256")
      mac.init(new SecretKeySpec(config.Coinbase.SecretKey.getBytes, "HmacSHA256"))
      new String(Hex.encodeHex(mac.doFinal(message.getBytes())))*/
      val decodedSecret = Base64.getDecoder.decode(config.Coinbase.SecretKey)
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(new SecretKeySpec(decodedSecret, "HmacSHA256"))
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

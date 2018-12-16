package net.ali.arbitrage.flows

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
import akka.stream.scaladsl._
import scala.concurrent._
import net.ali.json.JodaDateFormat.jodaDateReadsForMicroSeconds
import play.Logger
import akka.NotUsed
import com.google.inject.Provider
import play.api.libs.functional.syntax.toFunctionalBuilderOps

@Singleton
class CheckPriceTicksFlowProvider @Inject()(
  config: AliConfiguration,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem
) extends Provider[Flow[(PriceTick, PriceTick), ArbitrageOrder, NotUsed]] {
  
  @volatile var lastPriceTickOpt: Option[PriceTick] = None
  
  val arbitrageFlow: Flow[(PriceTick, PriceTick), ArbitrageOrder, NotUsed] = Flow[(PriceTick, PriceTick)]
    .map { case (priceTickA, priceTickB) =>
      val expirationTime = DateTime.now.minusSeconds(config.PriceTickExpiration.toSeconds.toInt)
      
      if (priceTickA.asOf.isBefore(expirationTime) || priceTickB.asOf.isBefore(expirationTime)) {
        Logger.info(s"EXPIRED - $priceTickA - $priceTickB")
        None
      } else {
        val grossProfitWhenBuyingFromA = config.CoinsQuantity * (priceTickB.bestBid - priceTickA.bestAsk)
        val grossProfitWhenBuyingFromB = config.CoinsQuantity * (priceTickA.bestBid - priceTickB.bestAsk)
        
        val totalFeesWhenBuyingFromA = 
          (config.CoinsQuantity * priceTickA.takerFee * priceTickA.bestAsk) +
          (config.CoinsQuantity * priceTickB.takerFee * priceTickB.bestBid)
          
        val totalFeesWhenBuyingFromB = 
          (config.CoinsQuantity * priceTickB.takerFee * priceTickB.bestAsk) +
          (config.CoinsQuantity * priceTickA.takerFee * priceTickA.bestBid)
          
        val netProfitWhenBuyingFromA = grossProfitWhenBuyingFromA - totalFeesWhenBuyingFromA
        val netProfitWhenBuyingFromB = grossProfitWhenBuyingFromB - totalFeesWhenBuyingFromB
        
        println(f"""
          ---------------------
          
          priceTickA: ${priceTickA.exchange}
          priceTickB: ${priceTickB.exchange}
          
          priceTickA.bestBid: ${priceTickA.bestBid}%2.10f
          priceTickB.bestBid: ${priceTickB.bestBid}%2.10f
        
          priceTickA.bestAsk: ${priceTickA.bestAsk}%2.10f
          priceTickB.bestAsk: ${priceTickB.bestAsk}%2.10f
            
          grossProfitWhenBuyingFromA: $grossProfitWhenBuyingFromA%2.10f
          grossProfitWhenBuyingFromB: $grossProfitWhenBuyingFromB%2.10f
          
          totalFeesWhenBuyingFromA: $totalFeesWhenBuyingFromA%2.10f
          totalFeesWhenBuyingFromB: $totalFeesWhenBuyingFromB%2.10f
          
          netProfitWhenBuyingFromA: $netProfitWhenBuyingFromA%2.10f
          netProfitWhenBuyingFromB: $netProfitWhenBuyingFromB%2.10f
          
          """)
        
        if (netProfitWhenBuyingFromA > config.ProfitThresholdComparedToFees * totalFeesWhenBuyingFromA) {
          Logger.info(s"PROFITABLE TO BUY ON ${priceTickA.exchange} - $priceTickA - $priceTickB")
          Some(
            ArbitrageOrder(
              buyingExchange = priceTickA.exchange, 
              buyingHttpClient = priceTickA.httpClient,
              sellingExchange = priceTickB.exchange,
              sellingHttpClient = priceTickB.httpClient
            )
          )
        } else if (netProfitWhenBuyingFromB > config.ProfitThresholdComparedToFees * totalFeesWhenBuyingFromB) {
          Logger.info(s"PROFITABLE TO BUY ON ${priceTickB.exchange} - $priceTickA - $priceTickB")
          Some(
            ArbitrageOrder(
              buyingExchange = priceTickB.exchange, 
              buyingHttpClient = priceTickB.httpClient,
              sellingExchange = priceTickA.exchange,
              sellingHttpClient = priceTickA.httpClient
            )
          )
        } else {
          Logger.info(s"NOT PROFITABLE - $priceTickA - $priceTickB")
          None
        }
      }
    }
    .collect {
      case Some(arbitrageOrder) => arbitrageOrder
    }
    
    
  
  override def get(): Flow[(PriceTick, PriceTick), ArbitrageOrder, NotUsed] = arbitrageFlow
  
}
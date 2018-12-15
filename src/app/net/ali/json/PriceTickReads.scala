package net.ali.json

import net.ali.RichImplicits.RichString
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json._
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Writes
import org.joda.time._
import net.ali._
import net.ali.json.JodaDateFormat._
import net.ali.models._
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceTickReads @Inject()(config: AliConfiguration) {
  
  val priceTickReadsFromCoinbase: Reads[PriceTick] = (
    (JsPath \ "best_bid").read[String] and
    (JsPath \ "best_ask").read[String]
  ) {
    (bestBid, bestAsk) => 
      PriceTick("coinbase", bestBid.toDouble, bestAsk.toDouble, asOf = DateTime.now, config.Coinbase.TakerFee)
  }
  
  val priceTickReadsFromBinance: Reads[PriceTick] = (
    (JsPath \ "b").read[String] and
    (JsPath \ "a").read[String] and
    (JsPath \ "E").read[Long]
  ) {
    (bestBid, bestAsk, asOfAsLong) => 
      PriceTick("binance", bestBid.toDouble, bestAsk.toDouble, new DateTime(asOfAsLong), config.Binance.TakerFee)
  }
  
}

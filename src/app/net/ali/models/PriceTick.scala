package net.ali.models

import org.joda.time._
import net.ali.http._

case class PriceTick(
  exchange: String,
  bestBid: Double,
  bestAsk: Double,
  asOf: DateTime,
  takerFee: Double,
  httpClient: HttpClient
)
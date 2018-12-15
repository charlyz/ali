package net.ali.models

import org.joda.time._

case class PriceTick(
  exchange: String,
  bestBid: Double,
  bestAsk: Double,
  asOf: DateTime,
  takerFee: Double
)
package net.ali.models

import org.joda.time._

case class PriceTick(
  bestBid: Double,
  bestAsk: Double,
  asOf: DateTime
)
package net.ali.models

import net.ali.http._

case class ArbitrageOrder(
  buyingExchange: String,
  buyingHttpClient: HttpClient,
  sellingExchange: String,
  sellingHttpClient: HttpClient
)
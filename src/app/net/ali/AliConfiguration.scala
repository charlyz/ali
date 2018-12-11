package net.ali

import java.net.InetAddress

import scala.util.Success
import scala.util.Try

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

@Singleton
class AliConfiguration @Inject()(config: Configuration) {
  
  val LeftPair = "LTC"
  val RightPair = "BTC"
  
  class CoinbaseInnerConfig {
    val TakerFee: Double = 0.003
    val WebSocketURL = "wss://ws-feed-public.sandbox.pro.coinbase.com"
  }
  val Coinbase = new CoinbaseInnerConfig
  
  class BinanceInnerConfig {
    val TakerFee: Double = 0.001
  }
  val Binance = new BinanceInnerConfig
  
}
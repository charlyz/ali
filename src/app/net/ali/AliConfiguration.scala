package net.ali

import java.net.InetAddress

import scala.util.Success
import scala.util.Try
import scala.concurrent.duration._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

@Singleton
class AliConfiguration @Inject()(config: Configuration) {
  
  val LeftPair = "LTC"
  val RightPair = "BTC"
  val PriceTickExpiration = 5.seconds
  
  class CoinbaseInnerConfig {
    val TakerFee: Double = 0.003
    val WebSocketURL = config.get[String]("ali.coinbase.web-socket-url")
  }
  val Coinbase = new CoinbaseInnerConfig
  
  class BinanceInnerConfig {
    val TakerFee: Double = 0.001
    val WebSocketURL = config.get[String]("ali.binance.web-socket-url")
  }
  val Binance = new BinanceInnerConfig
  
}
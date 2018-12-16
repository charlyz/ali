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
  val CoinsQuantity = 0.2
  val ProfitThresholdComparedToFees = 1 // Profits must be X times bigger than the total fees.
  val MaxOrdersPerMinute = 1
  
  class CoinbaseInnerConfig {
    val TakerFee: Double = 0.003
    val WebSocketURL = config.get[String]("ali.coinbase.web-socket-url")
    val SecretKey = config.get[String]("ali.coinbase.secret-key")
    val PublicKey = config.get[String]("ali.coinbase.public-key")
    val Passphrase = config.get[String]("ali.coinbase.passphrase")
    val HttpRequestTimeout = 10.seconds
    val ApiHostname = config.get[String]("ali.coinbase.api-hostname")
  }
  val Coinbase = new CoinbaseInnerConfig
  
  class BinanceInnerConfig {
    val TakerFee: Double = 0.001
    val WebSocketURL = config.get[String]("ali.binance.web-socket-url")
    val SecretKey = config.get[String]("ali.binance.secret-key")
    val PublicKey = config.get[String]("ali.binance.public-key")
    val ApiHostname = config.get[String]("ali.binance.api-hostname")
    val HttpRequestTimeout = 10.seconds
  }
  val Binance = new BinanceInnerConfig
  
  class BitfinexInnerConfig {
    val TakerFee: Double = 0.002
    val WebSocketURL = config.get[String]("ali.bitfinex.web-socket-url")
    val SecretKey = config.get[String]("ali.bitfinex.secret-key")
    val PublicKey = config.get[String]("ali.bitfinex.public-key")
    val ApiHostname = config.get[String]("ali.bitfinex.api-hostname")
    val HttpRequestTimeout = 10.seconds
  }
  val Bitfinex = new BitfinexInnerConfig
  
}
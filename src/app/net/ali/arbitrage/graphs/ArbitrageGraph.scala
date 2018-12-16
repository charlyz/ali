package net.ali.arbitrage.graphs

import javax.inject.Inject
import javax.inject.Singleton
import net.ali._
import akka.actor.ActorSystem
import akka.Done
import net.ali.models._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import org.joda.time.{ Duration => JodaDuration, _}
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import scala.concurrent._
import net.ali.exchanges.flows.CoinbaseFeedFlowProvider
import com.google.inject.Provider
import com.google.inject.name.Named
import akka.stream.Materializer
import net.ali.arbitrage.flows._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream._
import akka.NotUsed
import net.ali.http.clients._
import scala.concurrent.duration._
import play.Logger

@Singleton
class ArbitrageGraph @Inject()(
  config: AliConfiguration,
  @Named("coinbase-feed-flow") coinbaseFeedFlow: Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]],
  @Named("coinbase-subscribe-source") coinbaseSubscribeSource: Source[Message, Promise[Option[Message]]],
  @Named("binance-feed-flow") binanceFeedFlow: Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]],
  @Named("check-price-ticks-flow") checkPriceTicksFlow: Flow[(PriceTick, PriceTick), ArbitrageOrder, NotUsed],
  @Named("bitfinex-feed-flow") bitfinexFeedFlow: Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]],
  @Named("bitfinex-subscribe-source") bitfinexSubscribeSource: Source[Message, Promise[Option[Message]]],
  coinbaseClient: CoinbaseClient,
  binanceClient: BinanceClient,
  bitfinexClient: BitfinexClient,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem,
  implicit val mat: Materializer
) {
  
  val printingSink = Sink.ignore
  
  val throttleAndDiscardExcessFlow = Flow[PriceTick]
    .buffer(1, OverflowStrategy.dropBuffer)
    .throttle(3, 1.second, maximumBurst = 1, ThrottleMode.Shaping)
    
  val throttleAndCreateOrders = Flow[ArbitrageOrder]
    .buffer(1, OverflowStrategy.dropBuffer)
    .throttle(config.MaxOrdersPerMinute, 1.minute, maximumBurst = 1, ThrottleMode.Shaping)
    .mapAsync(1) { 
      case ArbitrageOrder(buyingExchange, buyingHttpClient, sellingExchange, sellingHttpClient) =>
        Logger.info(s"Buying on $buyingExchange and selling on $sellingExchange.")
        val buyOrderFuture = buyingHttpClient.createMarketBuyOrder()
        val sellOrderFuture = sellingHttpClient.createMarketSellOrder()
        buyOrderFuture.flatMap(_ => sellOrderFuture)
    }
   
  val binanceFeedSource = Source
    .maybe[Message]
    .viaMat(binanceFeedFlow)(Keep.right)
    .via(throttleAndDiscardExcessFlow)
  val coinbaseFeedSource = coinbaseSubscribeSource
    .viaMat(coinbaseFeedFlow)(Keep.right)
    .via(throttleAndDiscardExcessFlow)
  val bitfinexFeedSource = bitfinexSubscribeSource
    .viaMat(bitfinexFeedFlow)(Keep.right)
    .via(throttleAndDiscardExcessFlow)
  
  val ((binanceConnectionFuture, coinbaseConnectionFuture), _) = coinbaseFeedSource
    .zipMat(binanceFeedSource)(Keep.both)
    .viaMat(checkPriceTicksFlow)(Keep.both)
    .viaMat(throttleAndCreateOrders)(Keep.left)
    .toMat(printingSink)(Keep.left)
    .run()
    
    
    
  binanceConnectionFuture
    .flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }
    .onComplete(println)
    
  coinbaseConnectionFuture
    .flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }
    .onComplete(println)

}
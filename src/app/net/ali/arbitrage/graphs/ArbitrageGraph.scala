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
  coinbaseClient: CoinbaseClient,
  binanceClient: BinanceClient,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem,
  implicit val mat: Materializer
) {
  
  val printingSink = Sink.ignore
  
  val throttleAndDiscardExcessFlow = Flow[PriceTick]
    .buffer(1, OverflowStrategy.dropBuffer)
    .throttleEven(3, 1.second, ThrottleMode.Shaping)
    
  val throttleAndCreateOrders = Flow[ArbitrageOrder]
    .buffer(1, OverflowStrategy.dropBuffer)
    .throttleEven(config.MaxOrdersPerMinute, 1.minute, ThrottleMode.Shaping)
    .mapAsync(1) { 
      case ArbitrageOrder("coinbase", "binance") =>
        Logger.info("Buying on coinbase and selling on binance.")
        val buyOrderFuture = coinbaseClient.createMarketBuyOrder()
        val sellOrderFuture = binanceClient.createMarketSellOrder()
        buyOrderFuture.flatMap(_ => sellOrderFuture)
      case ArbitrageOrder("binance", "coinbase") =>
        Logger.info("Buying on binance and selling on coinbase.")
        val buyOrderFuture = binanceClient.createMarketBuyOrder()
        val sellOrderFuture = coinbaseClient.createMarketSellOrder()
        buyOrderFuture.flatMap(_ => sellOrderFuture)
      case arbitrageOrder => 
        throw new Exception(s"Order could not be created due to unexpected exchange names: $arbitrageOrder")
      
    }
   
  val binanceFeedSource = Source
    .maybe[Message]
    .viaMat(binanceFeedFlow)(Keep.right)
    .via(throttleAndDiscardExcessFlow)
  val coinbaseFeedSource = coinbaseSubscribeSource
    .viaMat(coinbaseFeedFlow)(Keep.right)
    .via(throttleAndDiscardExcessFlow)
  
  val ((binanceConnectionFuture, coinbaseConnectionFuture), _) = coinbaseFeedSource
    .zipMat(binanceFeedSource)(Keep.both)
    .viaMat(checkPriceTicksFlow)(Keep.both)
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
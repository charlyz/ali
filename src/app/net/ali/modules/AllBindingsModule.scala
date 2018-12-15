package net.ali.modules

import com.google.inject.AbstractModule
import com.google.inject.Singleton

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Named

import javax.inject.Inject
import net.ali._
import net.ali.arbitrage.graphs._
import net.ali.exchanges.flows._
import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import net.ali.http.clients._
import akka.stream.scaladsl._
import net.ali.exchanges.sources._
import akka.http.scaladsl.model.ws._
import net.ali.json._
import akka.stream.scaladsl._
import scala.concurrent._
import net.ali.models._
import net.ali.exchanges.flows.CoinbaseFeedFlowProvider
import com.google.inject.Provider
import com.google.inject.name._
import akka.stream.Materializer
import net.ali.arbitrage.flows._
import org.joda.time._
import play.api.Configuration
import play.api.Environment
import akka.NotUsed

@Singleton
class AllBindingsModule(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = { 
    bind(classOf[AliConfiguration])
    bind(classOf[PriceTickReads])
    bind(classOf[CoinbaseClient])
    bind(classOf[BinanceClient])
    
    bind(new TypeLiteral[Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]]]() {})
      .annotatedWith(Names.named("coinbase-feed-flow"))
      .toProvider(classOf[CoinbaseFeedFlowProvider])
      
    bind(new TypeLiteral[Source[Message, Promise[Option[Message]]]]() {})
      .annotatedWith(Names.named("coinbase-subscribe-source"))
      .toProvider(classOf[SubscribeToCoinbaseSourceProvider])
      
    //bind(classOf[CoinbaseFeedGraph])
    
    bind(new TypeLiteral[Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]]]() {})
      .annotatedWith(Names.named("binance-feed-flow"))
      .toProvider(classOf[BinanceFeedFlowProvider])
      
    //bind(classOf[BinanceFeedGraph])
      
    bind(new TypeLiteral[Flow[(PriceTick, PriceTick), ArbitrageOrder, NotUsed]]() {})
      .annotatedWith(Names.named("check-price-ticks-flow"))
      .toProvider(classOf[CheckPriceTicksFlowProvider])
      
    bind(classOf[ArbitrageGraph])
  }
  
}
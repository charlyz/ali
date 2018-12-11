package net.ali.modules

import com.google.inject.AbstractModule
import com.google.inject.Singleton

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Named

import javax.inject.Inject
import net.ali._
import net.ali.arbitrage.graphs.CoinbaseFeedGraph
import net.ali.exchanges.flows.CoinbaseFeedFlowProvider
import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import scala.concurrent._
import net.ali.exchanges.flows.CoinbaseFeedFlowProvider
import com.google.inject.Provider
import com.google.inject.name._
import akka.stream.Materializer
import akka.stream.scaladsl._
import play.api.Configuration
import play.api.Environment

@Singleton
class AllBindingsModule(
  environment: Environment,
  configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {
    //bind(classOf[CurrencyApiConfiguration])
    
    bind(classOf[AliConfiguration])
    
    bind(new TypeLiteral[Flow[Message, Message, Future[WebSocketUpgradeResponse]]]() {})
      .annotatedWith(Names.named("coinbase-feed-flow"))
      .toProvider(classOf[CoinbaseFeedFlowProvider])
      
    bind(classOf[CoinbaseFeedGraph])
  }
  
}
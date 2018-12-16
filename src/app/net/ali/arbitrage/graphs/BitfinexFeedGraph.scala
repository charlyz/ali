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
import org.joda.time._
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import scala.concurrent._
import net.ali.exchanges.flows.BitfinexFeedFlowProvider
import com.google.inject.Provider
import com.google.inject.name.Named
import akka.stream.Materializer

@Singleton
class BitfinexFeedGraph @Inject()(
  config: AliConfiguration,
  @Named("bitfinex-feed-flow") bitfinexFeedFlow: Flow[Message, PriceTick, Future[WebSocketUpgradeResponse]],
  @Named("bitfinex-subscribe-source") bitfinexSubscribeSource: Source[Message, Promise[Option[Message]]],
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem,
  implicit val mat: Materializer
) {

  val (upgradeResponse, closed) = bitfinexSubscribeSource
    .viaMat(bitfinexFeedFlow)(Keep.right) 
    .toMat(Sink.foreach(x => println("bitfinex: " + x)))(Keep.both) 
    .run()
    
  val connected = upgradeResponse.flatMap { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Future.successful(Done)
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  connected.onComplete(println)
  closed.onComplete { x => println("stream is done for bitfinex " + x) }
  
}
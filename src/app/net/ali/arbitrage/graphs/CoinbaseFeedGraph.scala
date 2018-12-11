package net.ali.arbitrage.graphs

import javax.inject.Inject
import javax.inject.Singleton
import net.ali._
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
import com.google.inject.name.Named
import akka.stream.Materializer

@Singleton
class CoinbaseFeedGraph @Inject()(
  config: AliConfiguration,
  @Named("coinbase-feed-flow") coinbaseFeedFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]],
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem,
  implicit val mat: Materializer
) {
  
  val subscribePayload = """
    |{
    |  "type": "subscribe",
    |  "product_ids": [
    |    "ETH-USD",
    |    "ETH-EUR"
    |  ],
    |  "channels": [
    |    "level2",
    |    "heartbeat",
    |    {
    |        "name": "ticker",
    |        "product_ids": [
    |            "ETH-BTC",
    |            "ETH-USD"
    |        ]
    |    }
    |  ]
    |}
    """.stripMargin
  
  val subscribeAndWaitSource = Source(List(TextMessage(subscribePayload))).concatMat(Source.maybe[Message])(Keep.right)
  
  val (upgradeResponse, closed) = subscribeAndWaitSource
    .viaMat(coinbaseFeedFlow)(Keep.right) 
    .toMat(Sink.foreach(println))(Keep.both) 
    .run()
    
  val connected = upgradeResponse.flatMap { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Future.successful(Done)
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  connected.onComplete(println)
  
}
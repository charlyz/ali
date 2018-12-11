package net.ali.exchanges.flows

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
import akka.stream.scaladsl.Flow
import scala.concurrent._
import com.google.inject.Provider

@Singleton
class CoinbaseFeedFlowProvider @Inject()(
  config: AliConfiguration,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem
) extends Provider[Flow[Message, Message, Future[WebSocketUpgradeResponse]]] {
  
  val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(config.Coinbase.WebSocketURL))
  
  override def get(): Flow[Message, Message, Future[WebSocketUpgradeResponse]] = {
    webSocketFlow
  }
  
}
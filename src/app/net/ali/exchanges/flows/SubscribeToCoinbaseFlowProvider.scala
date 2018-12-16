package net.ali.exchanges.sources

import javax.inject.Inject
import javax.inject.Singleton
import net.ali._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl.Flow
import akka.NotUsed
import scala.concurrent._
import com.google.inject.Provider
import akka.http.scaladsl.model.Uri.apply

@Singleton
class SubscribeToCoinbaseFlowProvider @Inject()(
  config: AliConfiguration,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem
) extends Provider[Flow[NotUsed, Message, NotUsed]] {
  
  val subscribePayload = s"""
    |{
    |  "type": "subscribe",
    |  "product_ids": [
    |    "${config.LeftPair}-${config.RightPair}"
    |  ],
    |  "channels": [
    |    "heartbeat",
    |    "ticker"
    |  ]
    |}
    """.stripMargin

  val subscribeFlow = Flow[NotUsed].map(_ => TextMessage(subscribePayload))
  
  override def get(): Flow[NotUsed, Message, NotUsed] = subscribeFlow
  
}
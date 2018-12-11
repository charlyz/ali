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
import scala.concurrent._
import com.google.inject.Provider
import akka.http.scaladsl.model.Uri.apply

@Singleton
class SubscribeToCoinbaseAndWaitSourceProvider @Inject()(
  config: AliConfiguration,
  implicit val ec: ExecutionContext,
  implicit val actorSystem: ActorSystem
) extends Provider[Source[Message, Promise[Option[Message]]]] {
  
  val subscribePayload = s"""
    |{
    |  "type": "subscribe",
    |  "channels": [
    |    "heartbeat",
    |    {
    |        "name": "ticker",
    |        "product_ids": [
    |            "${config.LeftPair}-${config.RightPair}",
    |        ]
    |    }
    |  ]
    |}
    """.stripMargin
  
  val subscribeAndWaitSource = Source(List(TextMessage(subscribePayload))).concatMat(Source.maybe[Message])(Keep.right)
  
  override def get(): Source[Message, Promise[Option[Message]]] = subscribeAndWaitSource
  
}
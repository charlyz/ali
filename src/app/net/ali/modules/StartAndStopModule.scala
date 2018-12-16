package net.ali.modules

import scala.concurrent.ExecutionContext

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import net.ali.http.clients._
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject.Inject
import play.api.inject.ApplicationLifecycle

// One the many modules configuring Guice to know
// what can be injected or not. As reminder, the Guice
// instance is created in CurrencyApiApplicationLoader.
// Play knows where to look for modules by looking at the
// configuration list `play.modules`.
class StartAndStopModule extends AbstractModule {
  override def configure(): Unit =
    bind(classOf[StartAndStopHook]).asEagerSingleton()
}

// We use a Guice component that we instantiate
// eagerly to start any asynchronous service.
@Singleton
class StartAndStopHook @Inject()(
  coinbaseClient: CoinbaseClient,
  binanceClient: BinanceClient,
  bitfinexClient: BitfinexClient,
  lifecycle: ApplicationLifecycle,
  implicit val actorSystem: ActorSystem,
  implicit val ec: ExecutionContext,
  implicit val mat: Materializer
) {
  //coinbaseClient.createLimitBuyOrder(0.001)
  //coinbaseClient.createMarketBuyOrder()
  //coinbaseClient.createMarketSellOrder()
  //binanceClient.createMarketBuyOrder()
  //bitfinexClient.createMarketBuyOrder()
}
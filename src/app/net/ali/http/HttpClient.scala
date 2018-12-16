package net.ali.http

import scala.concurrent.duration.Duration
import scala.language.reflectiveCalls
import scala.concurrent._
import play.api.Logger

trait HttpClient {
  
  def createMarketBuyOrder(): Future[Unit]
  def createMarketSellOrder(): Future[Unit]
  
  def logSendingRequestInfo(
    url: String,
    method: String,
    headersOpt: Option[Seq[(String, String)]],
    paramsOpt: Option[Seq[(String, String)]],
    bodyOpt: Option[String]
  ): Unit = {
    Logger.debug(
      List(
        "SENT",
        s"$method $url",
        s"QS $paramsOpt",
        s"BODY $bodyOpt",
        s"HEADERS $headersOpt"
      ).mkString(" | ")
    )
  }

  def logReceivingResponseInfo(
    url: String,
    method: String,
    headersOpt: Option[Seq[(String, String)]],
    paramsOpt: Option[Seq[(String, String)]],
    t0: Long,
    t1: Long,
    statusCode: Int,
    sizeOpt: Option[Long],
    responseBodyOpt: Option[String]
  ): Unit = {
    val responseTime = Duration.fromNanos(t1 - t0).toMillis

    val sanitizedResponseBodyOpt = responseBodyOpt match {
      case Some(sanitizedResponseBody) => Some(sanitizedResponseBody.replaceAll(" +", " ").replace("\n", ""))
      case _ => None
    }
    
    Logger.debug(
      List(
        "RCVD",
        s"$method $url",
        s"QS $paramsOpt",
        s"${responseTime}ms",
        s"$sizeOpt bytes",
        statusCode,
        s"HEADERS $headersOpt",
        s"RESPONSE $sanitizedResponseBodyOpt"
      ).mkString(" | ")
    )
  }

}

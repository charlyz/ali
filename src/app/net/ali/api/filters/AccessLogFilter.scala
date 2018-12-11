package net.ali.api.filters

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import net.ali.api.writeables._
import net.ali.RichImplicits._

import akka.stream.Materializer
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Logger
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results

// A filter in the Play library is way to execute
// some code for all the requests. In this case,
// we create a filter to log each request.
class AccessLogFilter @Inject()(
  jsonWriteables: JsonWriteables,
  implicit val mat: Materializer,
  implicit val ec: ExecutionContext
) extends Filter {

  import jsonWriteables._

  def apply(
    nextFilter: (RequestHeader) => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    // Before to execute the code in the controller,
    // we store the start time at which we received
    // the request.
    val startTime = System.currentTimeMillis

    // We call the next filter which eventually
    // will execute the controller code.
    nextFilter(requestHeader)
      .map { result =>
        // The controller returned a result. As the result is
        // already serialized in bytes, we don't know what's
        // in it. As we want to log the exceptions that
        // we support, such as Unauthorized or MissingParameter,
        // the controller adds that information to the X-Error-Description
        // header. If that header is set, we know what to log.
        val statusCode = result.header.status
        val errorMessageOpt = result.header.headers.get("X-Error-Description")
        logRequest(requestHeader, statusCode, startTime, errorMessageOpt)

        // Once we logged the request, we possibly remove the error
        // header so the user does not receive it.
        if (errorMessageOpt.isDefined) {
          result.removeHeaders("X-Error-Description")
        } else {
          result
        }
      }
      .recover {
        case exception =>
          // It is possible that an unexpected exception be populated
          // from the controller that would not even finish. In that
          // case, we log the request as usual, but also the full stack
          // trace for further investigation.
          val uuid = UUID.randomUUID().toString
          Logger.error(s"Unexpected error with UUID $uuid", exception)
          val errorMessageOpt = Some(s"See error with UUID: $uuid")
          logRequest(requestHeader, 500, startTime, errorMessageOpt)

          // We also build a standard pappy result for the user,
          // returning the name of the exception and its message,
          // but not the stack trace.
          Results.InternalServerError(exception)
      }
  }

  def logRequest(
    request: RequestHeader,
    status: Int,
    startTime: Long,
    errorMessageOpt: Option[String] = None
  ): Unit = {
    val requestTimeInMillis = System.currentTimeMillis - startTime
    val requestTimeAsString = s"${requestTimeInMillis}ms"

    val path = s"${request.method} ${request.path}"
    val qs = s"?${request.rawQueryString}"

    Logger.info(
      List(
        path,
        status,
        requestTimeAsString,
        qs,
        errorMessageOpt
      ).mkString(" | ")
    )
  }

}

package net.ali.api.handlers

import scala.concurrent.Future
import net.ali.api.models.Results._
import net.ali.api.models._
import net.ali.api.writeables._
import javax.inject.Inject
import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.NOT_FOUND
import play.api.http.Writeable
import play.api.mvc.RequestHeader
import play.api.mvc.Result

// An error handler in Play is way of catching exceptions
// that do not even reach the controller, such as invalid
// http request or non existing endpoint.
@Singleton
class ErrorHandler @Inject()(jsonWriteables: JsonWriteables) extends HttpErrorHandler {

  implicit val defaultExceptionWriteableForErrors: Writeable[Throwable] = jsonWriteables.writeAsExceptionResponse

  // In any case, we create a result that will be
  // caught by AccessLogFilter, that will then
  // log the erroneous request as any other.
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = Future.successful {
    statusCode match {
      case BAD_REQUEST => BadRequest(BadRequestException(message))
      case NOT_FOUND => NotFound(RouteNotFoundException)
      case _ => InternalServerError(ClientException(message))
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful {
    InternalServerError(exception)
  }
}

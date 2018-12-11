package net.ali

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.joda.time.DateTime

import play.api.mvc.Result

// This singleton contains few implicit classes
// that will decorate some classes, such as
// DateTime, with new functions. These new
// functions can be seen as self contained
// helping functions to reduce boiler plate.
object RichImplicits {

  implicit class RichException(value: Any) {
    def getSimpleName: String =
      value.getClass.getSimpleName.replace("$", "")
  }

  implicit class RichFuture[A](future: Future[A]) {
    final def await(duration: Duration = Duration.Inf): A =
      Await.result(future, duration)
  }

  implicit class RichDateTime[A](date: DateTime) {
    def toBeginningOfDay: DateTime =
      date
        .withHourOfDay(0)
        .withMinuteOfHour(0)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)

    def toEndOfDay: DateTime =
      date
        .withHourOfDay(23)
        .withMinuteOfHour(59)
        .withSecondOfMinute(59)
        .withMillisOfSecond(999)
  }

  implicit class RichThrowable(throwable: Throwable) {
    def getCausesWithoutStackTrace: String =
      getCausesWithoutStackTraceImpl(throwable)

    private def getCausesWithoutStackTraceImpl(
      throwable: Throwable,
      buffer: String = "",
      iterations: Int = 0
    ): String = {
      if (iterations == 5) {
        buffer
      } else {
        Option(throwable.getCause) match {
          case Some(cause) =>
            val message = Option(cause.getMessage) match {
              case Some(foundMessage) => s": $foundMessage"
              case _ => ""
            }
            getCausesWithoutStackTraceImpl(
              cause,
              s"$buffer Caused by ${throwable.getSimpleName}$message",
              iterations + 1
            )
          case _ => buffer
        }
      }
    }
  }

  implicit class RichResult[A](val r: Result) extends AnyVal {
    def removeHeaders(without: String*): Result =
      r.copy(header = r.header.copy(headers = r.header.headers -- without))
  }

  implicit class RichString(string: String) {

    def camelify: String = {
      val splitString = string.split("_").toList match {
        case head :: tail => head :: tail.map(_.capitalize)
        case x => x
      }

      splitString.mkString
    }

    def snakify: String = {
      string
        .foldLeft(new StringBuilder) {
          case (s, c) if Character.isUpperCase(c) => s.append("_").append(Character.toLowerCase(c))
          case (s, c) => s.append(c)
        }
        .toString
    }

  }

}

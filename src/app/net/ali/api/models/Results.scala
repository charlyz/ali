package net.ali.api.models


import net.ali.RichImplicits._

import play.api.http.Writeable
import play.api.mvc.Result
import play.api.mvc.Results.Status

// Controllers use these results to automatically
// create the X-Error-Description when returning
// an exception.
object Results {
  val NotFound = new Status(404) with ErrorDescriptionInHeaders
  val BadRequest = new Status(400) with ErrorDescriptionInHeaders
  val Unauthorized = new Status(401) with ErrorDescriptionInHeaders
  val InternalServerError = new Status(500) with ErrorDescriptionInHeaders
  val ServiceUnavailable = new Status(503) with ErrorDescriptionInHeaders
}

trait ErrorDescriptionInHeaders extends Status {
  def apply(ex: Throwable)(
    implicit writeable: Writeable[Throwable]
  ): Result = {
    val result = apply[Throwable](ex)
    result.withHeaders(
      "X-Error-Description" -> {
        s"${ex.getSimpleName}: ${ex.getMessage}${ex.getCausesWithoutStackTrace}"
      }
    )
  }
}

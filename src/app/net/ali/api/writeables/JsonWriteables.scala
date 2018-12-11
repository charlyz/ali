package net.ali.api.writeables

import akka.util.ByteString
import javax.inject.Inject
import javax.inject.Singleton
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.Codec
import net.ali.RichImplicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.JsNull
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.json.Writes

// A writeable is used to convert an instance
// to bytes. When a controller creates a result
// with a body as response, it needs a writeable
// to convert that body to bytes. Play supports
// the conversion JsValue (from play-json) to
// bytes. Here we add the support for the conversion
// from Throwable to bytes. So controllers can do
// things similar to InternalServerError(new Exception("error"))
@Singleton
class JsonWriteables @Inject()() {

  implicit def writeAsExceptionResponse(
    implicit codec: Codec
  ): Writeable[Throwable] = {
    Writeable(
      transform = { exception =>
        ByteString(Json.toBytes(buildErrorResponse(exception)))
      },
      contentType = Some("application/json")
    )
  }
  
  def buildErrorResponse(
    exception: Throwable
  ): JsObject = {
    JsObject(
      Seq(
        "error_id" -> JsString(exception.getSimpleName),
        "error_description" -> JsString(s"${exception.getMessage}${exception.getCausesWithoutStackTrace}")
      )
    )
  }

}

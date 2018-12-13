package net.ali.json

import org.joda.time.DateTime

import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json._
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import org.joda.time._
import org.joda.time.format._
import scala.util._

object JodaDateFormat {
  
  val datePatternForCoinbase = "yyyy-MM-dd'T'HH:mm:ss.SSS"
  val dateFormatterForCoinbase = DateTimeFormat.forPattern(datePatternForCoinbase)
  
  val defaultDatePattern = "yyyy-MM-dd HH:mm:ss.SSS"
  val defaultJodaDateWrites: Writes[DateTime] = jodaDateWrites(defaultDatePattern)
  val defaultJodaDateReads: Reads[DateTime] = jodaDateReads(defaultDatePattern)
  
  val jodaDateReadsForMicroSeconds: Reads[DateTime] = new Reads[DateTime] {
    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(dateAsString) => 
        val dateWithoutMicroSeconds = dateAsString.take(dateAsString.length - 4)
        Try(dateFormatterForCoinbase.parseDateTime(dateWithoutMicroSeconds)) match {
          case Success(date) => JsSuccess(date)
          case Failure(e) => 
            JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jodadate.format", dateAsString))))
        }
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.date"))))
    }

  }

}
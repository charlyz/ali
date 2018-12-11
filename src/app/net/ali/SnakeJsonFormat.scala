package net.ali

import net.ali.RichImplicits.RichString

import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes

object SnakeJsonFormat {
  
  def snakecaseReads[T](parentReads: JsValue => JsResult[T]): Reads[T] = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = {
      parentReads {
        json match {
          case obj: JsObject => 
            val camelifiedFields = obj.fields
              .map { case (key, value) =>
                key.camelify -> value
              }
            JsObject(camelifiedFields)
          case x => x
        }
      }
    }
  }

  def snakecaseWrites[T](parentWrites: T => JsValue): Writes[T] = new Writes[T] {
    def writes(o: T): JsValue = {
      parentWrites(o) match {
        case obj: JsObject => 
          val snakifiedFields = obj.fields
            .map { case (key, value) =>
              key.snakify -> value
            }
          JsObject(snakifiedFields)
        case x => x
      }
    }
  }
  
  def snakecase[T](reads: Reads[T]): Reads[T] = snakecaseReads(reads.reads)

  def snakecase[T](writes: Writes[T]): Writes[T] = snakecaseWrites(writes.writes)

  def snakecase[T](format: Format[T]): Format[T] = Format(snakecaseReads(format.reads), snakecaseWrites(format.writes))
  
}
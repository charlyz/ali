package net.ali.api.models

case class BadRequestException(
  message: String
) extends Exception(s"Request could not be parsed: $message")

case class BodyParsingException(cause: Throwable)
  extends Exception(
    "Body is invalid",
    cause
  )

case class ClientException(message: String)
  extends Exception(
    s"A client error occurred: $message"
  )

case object RouteNotFoundException extends Exception("Invalid route")

case object UnauthorizedException
  extends Exception(
    "You are not authorized to access this service"
  )

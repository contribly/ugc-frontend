package model.forms

import play.api.libs.json.Json

case class FlagSubmission(`type`: Option[String], notes: Option[String], email: Option[String])

object FlagSubmission {
  implicit val formats = Json.format[FlagSubmission]
}

package model

import play.api.libs.json._

case class RegistrationDetails(username: String, password: String)

object RegistrationDetails {
  implicit val formats = Json.format[RegistrationDetails]
}

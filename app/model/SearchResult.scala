package model

import java.util.Date

import play.api.libs.json.{Json, Format}

case class SearchResult(val numberFound: Long, val startIndex: Long, results: Seq[Report])

object SearchResult {
  implicit val formats: Format[SearchResult] = Json.format[SearchResult]
}

case class Report(val id: String, val headline: String, created: Date, noticeboard: Option[Noticeboard],
                  user: User, body: Option[String], image: Option[Image], tags: Seq[Tag], place: Option[Place])

object Report {
  implicit val formats: Format[Report] = Json.format[Report]
}

case class Image(val id: String)

object Image {
  implicit val formats: Format[Image] = Json.format[Image]
}

case class User(val id: String, val displayName: String, registered: Option[Date], via: Option[String])

object User {
  implicit val formats: Format[User] = Json.format[User]
}

case class LatLong(val latitude: Double, val longitude: Double)

object LatLong {
  implicit val formats: Format[LatLong] = Json.format[LatLong]
}

case class Osm(val osmId: Long, osmType: String)

object Osm {
  implicit val formats: Format[Osm] = Json.format[Osm]
}

case class Tag(val id: String, val name: String)

object Tag {
  implicit val formats: Format[Tag] = Json.format[Tag]
}

case class Place(val name: Option[String], val latLong: Option[LatLong], val osm: Option[Osm])

object Place {
  implicit val formats: Format[Place] = Json.format[Place]
}

case class Noticeboard(val id: String, val name: String, val description: Option[String],
                        val geoCodingResolution:  Option[String],
                        val endDate: Option[Date], val embargoDate: Option[Date], val scheduledDate: Option[Date])

object Noticeboard {
  implicit  val formats: Format[Noticeboard] = Json.format[Noticeboard]
}

case class NoticeboardSearchResult(val numberFound: Long, results: Seq[Noticeboard])

object NoticeboardSearchResult {
  implicit val formats: Format[NoticeboardSearchResult] = Json.format[NoticeboardSearchResult]
}
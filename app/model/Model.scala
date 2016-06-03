package model

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

class DateTimeFormat extends Reads[DateTime] {

  private val isoDateTime = ISODateTimeFormat.dateTime

  override def reads(json: JsValue): JsResult[DateTime] = {
    json match {
      case JsString(s) => JsSuccess(isoDateTime.parseDateTime(s))
      case _ => throw new RuntimeException()
    }
  }
}

object DateTimeFormat extends DateTimeFormat

case class Artifact(contentType: String, url: Option[String], width: Option[Int], height: Option[Int], label: String)

object Artifact {
  implicit val formats: Format[Artifact] = Json.format[Artifact]
}

case class Authority(client: Option[Client], user: Option[User])

object Authority {
  implicit val formats: Format[Authority] = Json.format[Authority]
}

case class Client(id: String, name: String)

object Client {
  implicit val formats: Format[Client] = Json.format[Client]
}

case class FlagType(id: String, name: String)

object FlagType {
  implicit val formats: Format[FlagType] = Json.format[FlagType]
}

case class LatLong(latitude: Double, longitude: Double)

object LatLong {
  implicit val formats: Format[LatLong] = Json.format[LatLong]
}

case class Media(id: String, `type`: Option[String])

object Media {
  implicit val formats: Format[Media] = Json.format[Media]
}

case class MediaUsage(media: Media, orientation: Option[Int], artifacts: Seq[Artifact])

object MediaUsage {
  implicit val formats: Format[MediaUsage] = Json.format[MediaUsage]
}

case class Noticeboard(id: String, name: String, description: Option[String],
                       geoCodingResolution:  Option[String],
                       endDate: Option[DateTime], embargoDate: Option[DateTime], scheduledDate: Option[DateTime],
                       moderated: Boolean, featured: Boolean, cover: Option[MediaUsage],
                       supportedMediaTypes: Set[String])

object Noticeboard {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val formats: Format[Noticeboard] = Json.format[Noticeboard]
}

case class NoticeboardSearchResult(numberFound: Long, results: Seq[Noticeboard])

object NoticeboardSearchResult {
  implicit val formats: Format[NoticeboardSearchResult] = Json.format[NoticeboardSearchResult]
}

case class Report(id: String, headline: String, created: DateTime, assignment: Option[Noticeboard],
                  body: Option[String], tags: Seq[Tag], place: Option[Place],
                  mediaUsages: Seq[MediaUsage], via: Authority)

case class Osm(osmId: Long, osmType: String)

object Osm {
  implicit val formats: Format[Osm] = Json.format[Osm]
}

case class Place(name: Option[String], latLong: Option[LatLong], osm: Option[Osm])

object Place {
  implicit val formats: Format[Place] = Json.format[Place]
}

object Report {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val formats: Format[Report] = Json.format[Report]
}

case class SearchResult(numberFound: Long, startIndex: Long, results: Seq[Report], refinements: Option[Map[String, Map[String, Long]]])

object SearchResult {
  implicit val formats: Format[SearchResult] = Json.format[SearchResult]
}

case class Tag(id: String, name: String)

object Tag {
  implicit val formats: Format[Tag] = Json.format[Tag]
}

case class User(id: String, username: String, displayName: String, registered: DateTime, via: Option[Client])

object User {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val formats: Format[User] = Json.format[User]
}
package model

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}
import play.api.libs.json._

class DateTimeFormat extends Reads[DateTime] {

  private val isoDateTimeNoMillis : DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()

  override def reads(json: JsValue): JsResult[DateTime] = {
    json match {
      case JsString(s) => JsSuccess(isoDateTimeNoMillis.parseDateTime(s))
      case _ => throw new RuntimeException()
    }
  }
}

object DateTimeFormat extends DateTimeFormat

case class Authority(val client: String)

object Authority {
  implicit val formats: Format[Authority] = Json.format[Authority]
}

case class SearchResult(val numberFound: Long, val startIndex: Long, results: Seq[Report])

object SearchResult {
  implicit val formats: Format[SearchResult] = Json.format[SearchResult]
}

case class Artifact(val contentType: String, val url: String, val width: Option[Int], val height: Option[Int], val label: String)

object Artifact {
  implicit val formats: Format[Artifact] = Json.format[Artifact]
}

case class Report(val id: String, val headline: String, created: DateTime, noticeboard: Option[Noticeboard],
                  user: User, body: Option[String], image: Option[Image], tags: Seq[Tag], place: Option[Place],
                  mediaUsages: Seq[MediaUsage], via: Option[Authority])

object Report {
  implicit val df: Reads[DateTime] = DateTimeFormat
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

case class Media(val id: String, val contentType: Option[String], val artifacts: Seq[Artifact])

object Media {
  implicit val formats: Format[Media] = Json.format[Media]
}

case class MediaUsage(val media: Media, val orientation: Option[Int])

object MediaUsage {
  implicit val formats: Format[MediaUsage] = Json.format[MediaUsage]
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
                        val endDate: Option[DateTime], val embargoDate: Option[DateTime], val scheduledDate: Option[DateTime],
                        val moderated: Boolean, val featured: Boolean, val cover: Option[Media],
                        val supportedMediaTypes: Set[String],
                        val contributions: Option[Int])

object Noticeboard {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit  val formats: Format[Noticeboard] = Json.format[Noticeboard]
}

case class NoticeboardSearchResult(val numberFound: Long, results: Seq[Noticeboard])

object NoticeboardSearchResult {
  implicit val formats: Format[NoticeboardSearchResult] = Json.format[NoticeboardSearchResult]
}
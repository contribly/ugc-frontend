package model

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
  implicit val reads: Reads[Artifact] = Json.reads[Artifact]
}

case class Assignment(id: String, name: String, description: Option[String],
                      geoCodingResolution:  Option[String],
                      ends: Option[DateTime], embargo: Option[DateTime], starts: Option[DateTime], featured: Boolean, cover: Option[MediaUsage])

object Assignment {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val reads: Reads[Assignment] = Json.reads[Assignment]
}

case class AssignmentSearchResult(numberFound: Long, results: Seq[Assignment])

case class Authority(client: Option[Client], user: Option[User])

object Authority {
  implicit val reads: Reads[Authority] = Json.reads[Authority]
}

case class Client(id: String, name: String)

object Client {
  implicit val reads: Reads[Client] = Json.reads[Client]
}

case class Contribution(id: String, headline: String, created: DateTime, assignment: Option[Assignment],
                        body: Option[String], tags: Seq[Tag], place: Option[Place],
                        mediaUsages: Seq[MediaUsage], via: Authority)

object Contribution {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val reads: Reads[Contribution] = Json.reads[Contribution]
}

case class ContributionSearchResult(numberFound: Long, results: Seq[Contribution], refinements: Option[Map[String, Map[String, Long]]])

case class FlagType(id: String, name: String)

object FlagType {
  implicit val reads: Reads[FlagType] = Json.reads[FlagType]
}

case class LatLong(latitude: Double, longitude: Double)

object LatLong {
  implicit val reads: Reads[LatLong] = Json.reads[LatLong]
}

case class Media(id: String, `type`: Option[String])

object Media {
  implicit val reads: Reads[Media] = Json.reads[Media]
}

case class MediaUsage(media: Media, orientation: Option[Int], artifacts: Seq[Artifact])

object MediaUsage {
  implicit val reads: Reads[MediaUsage] = Json.reads[MediaUsage]
}

case class Osm(osmId: Long, osmType: String)

object Osm {
  implicit val reads: Reads[Osm] = Json.reads[Osm]
}

case class Place(name: Option[String], latLong: Option[LatLong], osm: Option[Osm])

object Place {
  implicit val reads: Reads[Place] = Json.reads[Place]
}

case class Tag(id: String, name: String)

object Tag {
  implicit val reads: Reads[Tag] = Json.reads[Tag]
}

case class User(id: String, username: String, displayName: String, registered: DateTime, via: Option[Client])

object User {
  implicit val df: Reads[DateTime] = DateTimeFormat
  implicit val reads: Reads[User] = Json.reads[User]
}
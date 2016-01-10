package services.ugc

import java.io.File

import model._
import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.{Logger, Play}
import play.utils.UriEncoding
import views.html.helper

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

trait UGCService {

  val apiUrl: String
  val ownedBy: String
  val clientId: String
  val clientSecret: String

  val reportsUrl: String = apiUrl + "/reports"
  val mediaUrl: String = apiUrl + "/media"
  val noticeboardsUrl: String = apiUrl + "/noticeboards"
  val tokenUrl: String = apiUrl + "/token"
  val usersUrl: String = apiUrl + "/users"
  val verifyUrl: String = apiUrl + "/verify"

  val Ok: Int = 200
  val Accepted: Int = 202

  def noticeboard(id: String): Future[Noticeboard] = {
    val u = noticeboardsUrl + "/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Noticeboard]
      }
    }
  }

  def noticeboards(pageSize: Int, page: Int): Future[NoticeboardSearchResult] = {
    val u = noticeboardsUrl + "?pageSize=" + pageSize + "&page=" + page + "&ownedBy=" + ownedBy
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[NoticeboardSearchResult]
      }
    }
  }

  def owner(): Future[User] = {
    val u = apiUrl + "/users/" + ownedBy
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      r => {
        Json.parse(r.body).as[User]
      }
    }
  }

  def register(registrationDetails: RegistrationDetails): Future[Option[User]] = {
    WS.url(usersUrl).
      withHeaders(clientAuthHeader).
      post(Json.toJson(registrationDetails)).map {
      response => {
        if (response.status == Ok) {
          Logger.info("Register response: " + response.body)
          Some(Json.parse(response.body).as[User])
        } else {
          Logger.warn("Register request returned: " + response.status)
          None
        }
      }
    }
  }

  def reports(pageSize: Int, page: Int, tag: Option[String], noticeboard: Option[String], user: Option[String], hasMediaType: Option[String]): Future[SearchResult] = {
    val u = reportsUrl + "?ownedBy=" + ownedBy + "&pageSize=" + pageSize + "&page=" + page +
      tag.fold("")(t => "&tag=" + t) +
      noticeboard.fold("")(n => "&noticeboard=" + n) +
      user.fold("")(u => "&user=" + helper.urlEncode(u)) +
      hasMediaType.fold("")(mt => "&hasMediaType=" + mt)

    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[SearchResult]
      }
    }
  }

  def report(id: String): Future[Report] = {
    val u = reportsUrl + "/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Report]
      }
    }
  }

  def submit(headline: String, body: String, media: Option[Media], token: String): Future[Option[Report]] = {
    val applicationJsonHeader = "Content-Type" -> "application/json"

    val submissionJson = Json.obj("headline" -> headline,
      "body" -> body,
      "media" -> Json.toJson(Seq(media.map(m => Map("id" -> m.id)))))

    Logger.info("Submission JSON: " + submissionJson.toString())

    val eventualResponse = WS.url(reportsUrl).
      withHeaders(bearerTokenHeader(token), applicationJsonHeader).
      post(submissionJson)

    eventualResponse.map(r => {
      if (r.status == Ok) {
        Logger.info("Submission accepted: " + r.body)
        Some(Json.parse(r.body).as[Report])
      } else {
        Logger.info("Submission rejected: " + r.status + " " + r.body)
        None
      }
    })
  }

  def submitMedia(mf: File, token: String): Future[Option[Media]] = {
    val eventualResponse = WS.url(mediaUrl).
      withHeaders(bearerTokenHeader(token)).
      post(mf)

    eventualResponse.map(r => {
      Logger.info("Submit media status: " + r.status)
      if (r.status == Accepted) {
        Logger.info("Submit media response: " + r.body)
        Some(Json.parse(r.body).as[Media])
      } else {
        None
      }
    })
  }

  def tag(id: String): Future[Tag] = {
    val u = apiUrl + "/tags/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Tag]
      }
    }
  }

  def tags(): Future[Seq[Tag]] = {
    val u = apiUrl + "/tags?ownedBy=" + ownedBy
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Seq[Tag]]
      }
    }
  }

  def token(username: String, password: String): Future[Option[String]] = {
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val eventualResponse = WS.url(tokenUrl).
      withHeaders(clientAuthHeader, formUrlEncodedContentTypeHeader).
      post("grant_type=password&username=" + username + "&password=" + password)

    eventualResponse.map(r => {
      if (r.status == Ok) {
        val responseJson = Json.parse(r.body)
        (responseJson \ "access_token").asOpt[String]
      } else {
        Logger.info(r.status + ": " + r.body)
        None
      }
    })
  }

  def user(id: String): Future[User] = {
    val u = usersUrl + "/" + UriEncoding.encodePathSegment(id, "UTF-8")
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[User]
      }
    }
  }

  def verify(token: String): Future[Option[User]] = {
    val authorizationHeader = bearerTokenHeader(token)

    WS.url(verifyUrl).withHeaders(authorizationHeader).
      post("").map {
      r => {
        if (r.status == Ok) {
          Some(Json.parse(r.body).as[User])
        } else {
          Logger.info(r.status + ": " + r.body)
          None
        }
      }
    }
  }

  private def bearerTokenHeader(token: String): (String, String) = {
    ("Authorization" -> ("Bearer " + token))
  }

  private def clientAuthHeader: (String, String) = {
    "Authorization" -> ("Basic " + Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes()))
  }

}

object UGCService extends UGCService {

  override lazy val apiUrl: String = Play.configuration.getString("ugc.api.url").get
  override lazy val ownedBy: String = Play.configuration.getString("ugc.user").get
  override lazy val clientId: String = Play.configuration.getString("ugc.client.id").get
  override lazy val clientSecret: String = Play.configuration.getString("ugc.client.secret").get

}


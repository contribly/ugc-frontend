package services.ugc

import model._
import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.{Logger, Play}

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

trait UGCService {

  val apiUrl: String
  val user: String
  val clientId: String
  val clientSecret: String

  val reportsUrl: String = apiUrl + "/reports"
  val noticeboardsUrl: String = apiUrl + "/noticeboards"
  val tokenUrl: String = apiUrl + "/token"
  val verifyUrl: String = apiUrl + "/verify"

  val Ok: Int = 200

  def reports(pageSize: Int, page: Int, tag: Option[String]): Future[SearchResult] = {
    val u = reportsUrl + "?pageSize=" + pageSize + "&page=" + page + tag.fold("")(t => "&tag=" + t)
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

  def token(username: String, password: String): Future[Option[String]] = {

    val clientAuthorizationHeader = "Authorization" -> ("Basic " + Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes()))
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val eventualResponse = WS.url(tokenUrl).
      withHeaders(clientAuthorizationHeader, formUrlEncodedContentTypeHeader).
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

  def verify(token: String): Future[Option[User]] = {
    val authorizationHeader = ("Authorization" -> ("Bearer " + token))

    WS.url(verifyUrl).withHeaders(authorizationHeader).
      post("").map {
      r => {
        if (r.status == Ok) {
          val jsonResponse: JsValue = Json.parse(r.body)
          (jsonResponse \ "user").asOpt[User]
        } else {
          Logger.info(r.status + ": " + r.body)
          None
        }
      }
    }
  }
  
  def noticeboards(pageSize: Int, page: Int): Future[NoticeboardSearchResult] = {
    val u = noticeboardsUrl + "?pageSize=" + pageSize + "&page=" + page + "&noticeboardOwnedBy=" + user
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[NoticeboardSearchResult]
      }
    }
  }

  def noticeboard(id: String): Future[Noticeboard] = {
    val u = noticeboardsUrl + "/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Noticeboard]
      }
    }
  }

  def owner(): Future[User] = {
    val u = apiUrl + "/users/" + "wellynews"
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[User]
      }
    }
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
    val u = apiUrl + "/tags"
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Seq[Tag]]
      }
    }
  }

}

object UGCService extends UGCService {

  override lazy val apiUrl: String = Play.configuration.getString("ugc.api.url").get
  override lazy val user: String = Play.configuration.getString("ugc.user").get
  override lazy val clientId: String = Play.configuration.getString("ugc.client.id").get
  override lazy val clientSecret: String = Play.configuration.getString("ugc.client.secret").get

}


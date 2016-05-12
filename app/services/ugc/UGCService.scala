package services.ugc

import java.io.File

import com.netaporter.uri.dsl._
import model._
import model.forms.{FlagSubmission, RegistrationDetails}
import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.{Logger, Play}
import play.utils.UriEncoding

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

trait UGCService {

  val apiUrl: String
  val ownedBy: String
  val clientId: String
  val clientSecret: String

  val applicationJsonHeader = "Content-Type" -> "application/json"

  val reportsUrl: String = apiUrl + "/reports"
  val mediaUrl: String = apiUrl + "/media"
  val noticeboardsUrl: String = apiUrl + "/noticeboards"
  val tokenUrl: String = apiUrl + "/token"
  val usersUrl: String = apiUrl + "/users"
  val verifyUrl: String = apiUrl + "/verify"

  val Ok: Int = 200
  val Accepted: Int = 202

  def flagTypes: Future[Seq[FlagType]] = {
    WS.url(apiUrl + "/flag-types").get.map { r =>
      Json.parse(r.body).as[Seq[FlagType]]
    }
  }

  def noticeboard(id: String): Future[Noticeboard] = {
    WS.url(noticeboardsUrl + "/" + id).get.map { r =>
      Json.parse(r.body).as[Noticeboard]
    }
  }

  def noticeboards(pageSize: Int, page: Int): Future[NoticeboardSearchResult] = {
    val params = Seq(
      "pageSize" -> pageSize,
      "page" -> page,
      "ownedBy" -> ownedBy
      )

    WS.url((noticeboardsUrl).addParams(params)).get.map { r =>
      Json.parse(r.body).as[NoticeboardSearchResult]
    }
  }

  def owner(): Future[Option[User]] = {
    user(ownedBy)
  }

  def register(registrationDetails: RegistrationDetails): Future[Either[String, User]] = {
    WS.url(usersUrl).withHeaders(clientAuthHeader).
      post(Json.toJson(registrationDetails)).map { r =>
        if (r.status == Ok) {
          Right(Json.parse(r.body).as[User])
        } else {
          Logger.warn("Register request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
    }
  }

  def reports(pageSize: Int, page: Int, tag: Option[String], noticeboard: Option[String], user: Option[String],
              hasMediaType: Option[String], token: Option[String]): Future[SearchResult] = {

    val params = Seq(
      Some("ownedBy" -> ownedBy),
      Some("pageSize" -> pageSize),
      Some("page" -> page),
      tag.map(t => "tag" -> t),
      noticeboard.map(n => "noticeboard" -> n),
      user.map(u => "user" -> u),
      hasMediaType.map(mt => "hasMediaType" -> mt)
    ).flatten

    val u = (reportsUrl).addParams(params)
    Logger.info("Fetching from url: " + u)
    val url = WS.url(u)
    val withToken = token.fold(url){ t => url.withHeaders(bearerTokenHeader(t))}
    withToken.get.map {
      response => {
        Json.parse(response.body).as[SearchResult]
      }
    }
  }

  def report(id: String): Future[Report] = {
    WS.url(reportsUrl + "/" + id).get.map { r =>
      Json.parse(r.body).as[Report]
    }
  }

  def submit(headline: String, body: String, media: Option[Media], token: String): Future[Option[Report]] = {

    val submissionJson = Json.obj("headline" -> headline,
      "body" -> body,
      "media" -> media.map(m => Json.toJson(Seq(Map("id" -> m.id))))
    )

    Logger.info("Report submission JSON: " + submissionJson.toString())

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

  def submitFlag(reportId: String, flagSubmission: FlagSubmission, token: Option[String]): Future[Unit] = {
    val headers = Seq(Some(applicationJsonHeader), token.map(t => bearerTokenHeader(t))).flatten

    WS.url(reportsUrl + "/" + reportId + "/flag").
      withHeaders(headers: _*).
      post(Json.toJson(flagSubmission)).map { response =>
      Logger.info("Response: " + response)
    }
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
    WS.url(apiUrl + "/tags/" + id).get.map { r =>
      Json.parse(r.body).as[Tag]
    }
  }

  def tags(): Future[Seq[Tag]] = {
    WS.url((apiUrl + "/tags").addParam("ownedBy", ownedBy)).get.map { r =>
      Json.parse(r.body).as[Seq[Tag]]
    }
  }

  def token(username: String, password: String): Future[Either[String, String]] = {
    Logger.info("Requesting token for: " + Seq(username, password, clientId).mkString(", "))
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val params = Map(
      "grant_type" -> Seq("password"),
      "username" -> Seq(username),
      "password" -> Seq(password)
    )

    WS.url(tokenUrl).
      withHeaders(clientAuthHeader, formUrlEncodedContentTypeHeader).
      post(params).map{ r =>
        if (r.status == Ok) {
          val expectedTokenOnResponse = (Json.parse(r.body) \ "access_token").asOpt[String]
          Either.cond(expectedTokenOnResponse.nonEmpty, expectedTokenOnResponse.get, "No token seen on sign in response")

        } else {
          Logger.info("Token request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
      }
  }

  def tokenGoogle(googleToken: String): Future[Either[String, String]] = {
    Logger.info("Requesting token for Google access token: " + googleToken)
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val params = Map(
      "grant_type" -> Seq("google"),
      "token" -> Seq(googleToken)
    )

    WS.url(tokenUrl).
      withHeaders(clientAuthHeader, formUrlEncodedContentTypeHeader).
      post(params).map{ r =>
        if (r.status == Ok) {
          val expectedTokenOnResponse = (Json.parse(r.body) \ "access_token").asOpt[String]
          Either.cond(expectedTokenOnResponse.nonEmpty, expectedTokenOnResponse.get, "No token seen on sign in response")

        } else {
          Logger.info("Google token request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
      }
  }

  def tokenFacebook(facebookAccessToken: String): Future[Either[String, String]] = {
    Logger.info("Requesting token for facebook access token: " + facebookAccessToken)
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val params = Map(
      "grant_type" -> Seq("facebook"),
      "token" -> Seq(facebookAccessToken)
    )

    WS.url(tokenUrl).
      withHeaders(clientAuthHeader, formUrlEncodedContentTypeHeader).
      post(params).map{ r =>
        if (r.status == Ok) {
          val expectedTokenOnResponse = (Json.parse(r.body) \ "access_token").asOpt[String]
          Either.cond(expectedTokenOnResponse.nonEmpty, expectedTokenOnResponse.get, "No token seen on sign in response")

        } else {
          Logger.info("Facebook token request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
      }
  }

  def tokenTwitter(token: String, secret: String): Future[Either[String, String]] = {
    Logger.info("Requesting token for Twitter access token: " + token)
    val formUrlEncodedContentTypeHeader = "Content-Type" -> "application/x-www-form-urlencoded"

    val params = Map(
      "grant_type" -> Seq("twitter"),
      "token" -> Seq(token),
      "secret" -> Seq(secret)
    )

    WS.url(tokenUrl).
      withHeaders(clientAuthHeader, formUrlEncodedContentTypeHeader).
      post(params).map{ r =>
        if (r.status == Ok) {
          val expectedTokenOnResponse = (Json.parse(r.body) \ "access_token").asOpt[String]
          Either.cond(expectedTokenOnResponse.nonEmpty, expectedTokenOnResponse.get, "No token seen on sign in response")

        } else {
          Logger.info("Twitter token request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
      }
  }

  def user(id: String): Future[Option[User]] = {
    WS.url(usersUrl + "/" + UriEncoding.encodePathSegment(id, "UTF-8")).get.map { r =>
      if (r.status == Ok) {
        Some(Json.parse(r.body).as[User])
      } else {
        None
      }
    }
  }

  def verify(token: String): Future[Option[User]] = {
    val authorizationHeader = bearerTokenHeader(token)

    WS.url(verifyUrl).withHeaders(authorizationHeader).
      post("").map { r =>
      if (r.status == Ok) {
        Some(Json.parse(r.body).as[User])
      } else {
        Logger.info(r.status + ": " + r.body)
        None
      }

    }
  }

  private def bearerTokenHeader(token: String): (String, String) = {
    "Authorization" -> ("Bearer " + token)
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


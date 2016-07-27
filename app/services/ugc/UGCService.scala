package services.ugc

import java.io.File
import javax.inject.Inject

import com.netaporter.uri.dsl._
import model._
import model.forms.{FlagSubmission, RegistrationDetails}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.Results
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

class UGCService @Inject() (configuration: Configuration, ws: WSClient) {

  val apiUrl = configuration.getString("ugc.api.url").get
  val ownedBy = configuration.getString("ugc.user").get
  val clientId = configuration.getString("ugc.client.id").get
  val clientSecret = configuration.getString("ugc.client.secret").get

  val applicationJsonHeader = "Content-Type" -> "application/json"

  val assignmentsUrl = apiUrl + "/assignments"
  val contributionsUrl = apiUrl + "/contributions"
  val contributionRefinementsUrl = apiUrl + "/contribution-refinements"
  val mediaUrl = apiUrl + "/media"
  val tokenUrl = apiUrl + "/token"
  val usersUrl = apiUrl + "/users"
  val verifyUrl = apiUrl + "/verify"

  val Ok: Int = 200
  val Accepted: Int = 202

  def flagTypes: Future[Seq[FlagType]] = {
    ws.url(apiUrl + "/flag-types").get.map { r =>
      Json.parse(r.body).as[Seq[FlagType]]
    }
  }

  def assignment(id: String): Future[Assignment] = {
    ws.url(assignmentsUrl / id).get.map { r =>
      Json.parse(r.body).as[Assignment]
    }
  }

  def assignments(pageSize: Int, page: Int): Future[AssignmentSearchResult] = {
    val params = Seq(
      "pageSize" -> pageSize,
      "page" -> page,
      "ownedBy" -> ownedBy
      )

    ws.url(assignmentsUrl.addParams(params)).get.map { r =>
      val assignments = Json.parse(r.body).as[Seq[Assignment]]
      AssignmentSearchResult(r.header("X-total-count").map(c => c.toLong).getOrElse(0), assignments)
    }
  }

  def owner(): Future[Option[User]] = {
    user(ownedBy)
  }

  def register(registrationDetails: RegistrationDetails): Future[Either[String, User]] = {
    ws.url(usersUrl).withHeaders(clientAuthHeader).
      post(Json.toJson(registrationDetails)).map { r =>
        if (r.status == Ok) {
          Right(Json.parse(r.body).as[User])
        } else {
          Logger.warn("Register request failed: " + r.status + " / " + r.body)
          Left(r.body)
        }
    }
  }

  def contribution(id: String, token: Option[String]): Future[Option[Contribution]] = {
    val reportRequest: WSRequest = ws.url(contributionsUrl / id)
    val withToken = token.fold(reportRequest) { t => reportRequest.withHeaders(bearerTokenHeader(t)) }

    withToken.get.map { r =>
      r.status match {
        case 200 =>
          Some(Json.parse(r.body).as[Contribution])
        case _ =>
          Logger.info("Non 200 status for fetch report: " + r.status + " / " + r.body)
          None
      }
    }
  }

  def contributions(pageSize: Int,
                    page: Option[Int] = None,
                    tag: Option[String] = None,
                    assignment: Option[String] = None,
                    user: Option[String] = None,
                    mediaType: Option[String] = None,
                    state: Option[String] = None,
                    refinements: Option[Seq[String]] = None,
                    token: Option[String] = None): Future[Option[ContributionSearchResult]] = {

    val params = Seq(
      Some("ownedBy" -> ownedBy),
      Some("pageSize" -> pageSize),
      Some("page" -> page),
      tag.map(t => "tag" -> t),
      assignment.map(n => "assignment" -> n),
      user.map(u => "user" -> u),
      mediaType.map(mt => "mediaType" -> mt),
      state.map(s => "state" -> s),
      refinements.map(r => ("refinements", r.mkString(",")))
    ).flatten

    val url = ws.url(contributionsUrl.addParams(params))
    val withToken = token.fold(url) { t => url.withHeaders(bearerTokenHeader(t)) }
    withToken.get.map { r =>
      r.status match {
        case 200 =>
          val contributions = Json.parse(r.body).as[Seq[Contribution]]
          Some(ContributionSearchResult(r.header("X-total-count").map(c => c.toLong).getOrElse(0), contributions, None))
        case _ =>
          None
      }
    }
  }

  def contributionRefinements(tag: Option[String] = None,
                    assignment: Option[String] = None,
                    user: Option[String] = None,
                    mediaType: Option[String] = None,
                    state: Option[String] = None,
                    refinements: Seq[String],
                    token: Option[String] = None): Future[Option[Map[String, Map[String, Long]]]] = {

    val params = Seq(
      Some("ownedBy" -> ownedBy),
      tag.map(t => "tag" -> t),
      assignment.map(n => "assignment" -> n),
      user.map(u => "user" -> u),
      mediaType.map(mt => "mediaType" -> mt),
      state.map(s => "state" -> s),
      Some("refinements", refinements.mkString(","))
    ).flatten

    val url = ws.url(contributionRefinementsUrl.addParams(params))
    val withToken = token.fold(url) { t => url.withHeaders(bearerTokenHeader(t)) }
    withToken.get.map { r =>
      r.status match {
        case 200 =>
          Logger.info("!!!!!!!!! " + r.body)
          Some(Json.parse(r.body).as[Map[String, Map[String, Long]]])
        case _ =>
          None
      }
    }
  }

  def submit(headline: String, body: String, media: Option[Media], token: String): Future[Option[Contribution]] = {

    val submissionJson = Json.obj("headline" -> headline,
      "body" -> body,
      "media" -> media.map(m => Json.toJson(Seq(Map("id" -> m.id))))
    )

    val eventualResponse = ws.url(contributionsUrl).
      withHeaders(bearerTokenHeader(token), applicationJsonHeader).
      post(submissionJson)

    eventualResponse.map(r => {
      if (r.status == Ok) {
        Logger.info("Submission accepted: " + r.body)
        Some(Json.parse(r.body).as[Contribution])
      } else {
        Logger.info("Submission rejected: " + r.status + " " + r.body)
        None
      }
    })
  }

  def submitFlag(reportId: String, flagSubmission: FlagSubmission, token: Option[String]): Future[Unit] = {
    val headers = Seq(Some(applicationJsonHeader), token.map(t => bearerTokenHeader(t))).flatten

    ws.url(contributionsUrl / reportId / "flag").
      withHeaders(headers: _*).
      post(Json.toJson(flagSubmission)).map { response =>
      Logger.info("Response: " + response)
    }
  }

  def submitMedia(mf: File, token: String): Future[Option[Media]] = {
    val eventualResponse = ws.url(mediaUrl).
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
    ws.url(apiUrl + "/tags/" + id).get.map { r =>
      Json.parse(r.body).as[Tag]
    }
  }

  def tags(): Future[Seq[Tag]] = {
    ws.url((apiUrl + "/tags").addParam("ownedBy", ownedBy)).get.map { r =>
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

    ws.url(tokenUrl).
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

    ws.url(tokenUrl).
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

    ws.url(tokenUrl).
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

    ws.url(tokenUrl).
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
    ws.url(usersUrl / id).get.map { r =>
      if (r.status == Ok) {
        Some(Json.parse(r.body).as[User])
      } else {
        None
      }
    }
  }

  def verify(token: String): Future[Option[Authority]] = {
    ws.url(verifyUrl).withHeaders(bearerTokenHeader(token)).post(Results.EmptyContent()).map { r =>
      r.status match {
        case Ok => Some(Json.parse(r.body).as[Authority])
        case _ => None
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
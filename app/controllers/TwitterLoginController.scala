package controllers

import _root_.twitter4j.conf.{Configuration, ConfigurationBuilder}
import akka.serialization.Serialization
import play.api.libs.json.Json
import twitter4j.{TwitterException, Twitter, TwitterFactory}
import play.api.Play.current
import play.api.mvc.{Session, Action, Controller}
import play.api.{Logger, Play}
import services.ugc.UGCService
import twitter4j.auth.{AccessToken, RequestToken}

import scala.concurrent.Future

trait TwitterLoginController extends Controller {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val rootUrl: String
  val consumerKey: String
  val consumerSecret: String

  val callbackUrl = rootUrl + routes.TwitterLoginController.callback(None, None)
  val TwitterRequestTokenSessionKey: String = "twitter-request-token"

  def redirect() = Action.async { request =>

    val twitterApi = getTwitterApi

    val requestToken: RequestToken = twitterApi.getOAuthRequestToken(callbackUrl)
    val serializedRequestToken = Json.toJson(Map(
      "token" -> requestToken.getToken,
      "secret" -> requestToken.getTokenSecret
    )
    ).toString()

    val loginDialogUrlString = requestToken.getAuthenticationURL

    Logger.info("Redirecting to Twitter login dialog: " + loginDialogUrlString)
    val withRequestToken: Session = request.session + (TwitterRequestTokenSessionKey, serializedRequestToken)
    Future.successful(Redirect(loginDialogUrlString).withSession(withRequestToken))
  }

  def callback(oauth_token: Option[String], oauth_verifier: Option[String]) = Action.async { request =>
      Logger.info("Received Twitter callback: " + oauth_token + ", " + oauth_verifier)

      oauth_token.map { t =>
        oauth_verifier.map { v =>

          request.session.get(TwitterRequestTokenSessionKey).map { srt =>
            val rtMap = Json.parse(srt).as[Map[String, String]]
            val requestToken = new RequestToken(rtMap.get("token").getOrElse(""), rtMap.get("secret").getOrElse(""))
            try {
              Logger.info("Exchanging Twitter request token for access token: " + requestToken + ", " + v)
              val twitterApi: Twitter = getTwitterApi
              val twitterAccessToken: AccessToken = twitterApi.getOAuthAccessToken(requestToken, v)
              Logger.info("Got Twitter access token: " + twitterAccessToken.getToken)

            } catch {
              case te: TwitterException => {
                Logger.warn("Twitter exception while trying to exchange for access token", te)
              }
              case e: Exception => {
                Logger.error("Exception while trying to exchange for access token", e)
              }
            }
          }
        }
      }

    val withClearedRequestToken: Session = request.session - (TwitterRequestTokenSessionKey)

    Future.successful(Redirect(routes.Application.index(None, None)).withSession(withClearedRequestToken))
  }

  private def getTwitterApi: Twitter = {
    def buildConfig: Configuration = {
      new ConfigurationBuilder().setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).build
    }
    return new TwitterFactory(buildConfig).getInstance
  }

}

object TwitterLoginController extends TwitterLoginController {
  override lazy val rootUrl = Play.configuration.getString("root.url").get
  override lazy val consumerKey = Play.configuration.getString("twitter.consumer.key").get
  override lazy val consumerSecret = Play.configuration.getString("twitter.consumer.secret").get
}

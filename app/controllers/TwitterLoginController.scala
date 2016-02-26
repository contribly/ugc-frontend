package controllers

import _root_.twitter4j.conf.{Configuration, ConfigurationBuilder}
import twitter4j.{TwitterException, Twitter, TwitterFactory}
import play.api.Play.current
import play.api.mvc.{Action, Controller}
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

  def redirect() = Action.async { request =>

    val twitterApi = getTwitterApi

    val requestToken: RequestToken = twitterApi.getOAuthRequestToken(callbackUrl)
    //requestTokens.put(requestToken.getToken, requestToken)  TODO need state
    val loginDialogUrlString = requestToken.getAuthorizationURL

    Logger.info("Redirecting to Twitter login dialog: " + loginDialogUrlString)
    Future.successful(Redirect(loginDialogUrlString))
  }

  def callback(oauth_token: Option[String], oauth_verifier: Option[String]) = Action.async { request =>
      Logger.info("Recieved Twitter callback: " + oauth_token + ", " + oauth_verifier)

      oauth_token.map { t =>
        oauth_verifier.map { v =>

          val twitterApi: Twitter = getTwitterApi

          val requestToken: RequestToken = twitterApi.getOAuthRequestToken(callbackUrl)

          try {
            val twitterAccessToken: AccessToken = twitterApi.getOAuthAccessToken(requestToken, v)
            Logger.info("Got access token: " + twitterAccessToken.getToken)

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

      Future.successful(Redirect(routes.Application.index(None, None)))   // TODO implement
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

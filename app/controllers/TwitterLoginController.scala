package controllers

import _root_.twitter4j.conf.{Configuration, ConfigurationBuilder}
import _root_.twitter4j.{Twitter, TwitterFactory}
import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.{Logger, Play}
import services.ugc.UGCService
import twitter4j.auth.RequestToken

import scala.concurrent.Future

trait TwitterLoginController extends Controller {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val rootUrl: String
  val consumerKey: String
  val consumerSecret: String

  val callbackUrl = rootUrl + routes.FacebookLoginController.callback(None, None, None, None)

  def redirect() = Action.async { request =>

    def getTwitterApi: Twitter = {
      return new TwitterFactory(buildConfig).getInstance
    }

    def buildConfig: Configuration = {
      new ConfigurationBuilder().setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).build
    }

    val twitterApi = getTwitterApi

    val requestToken: RequestToken = twitterApi.getOAuthRequestToken(callbackUrl)
    //requestTokens.put(requestToken.getToken, requestToken)  TODO need state
    val loginDialogUrlString = requestToken.getAuthorizationURL

    Logger.info("Redirecting to Twitter login dialog: " + loginDialogUrlString)
    Future.successful(Redirect(loginDialogUrlString))
  }

  def callback(oauth_token: Option[String], oauth_verifier: Option[String]) = Action.async { request =>
      Future.successful(Redirect(routes.Application.index(None, None)))   // TODO implement
  }

}

object TwitterLoginController extends TwitterLoginController {
  override lazy val rootUrl = Play.configuration.getString("root.url").get
  override lazy val consumerKey = Play.configuration.getString("twitter.consumer.key").get
  override lazy val consumerSecret = Play.configuration.getString("twitter.consumer.secret").get
}

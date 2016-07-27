package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Session}
import play.api.{Configuration, Logger}
import services.ugc.UGCService
import twitter4j.auth.{AccessToken, RequestToken}
import twitter4j.{Twitter, TwitterException, TwitterFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TwitterLoginController @Inject() (configuration: Configuration, ugcService: UGCService, signedInUserService: SignedInUserService) extends Controller {

  val rootUrl = configuration.getString("root.url").get
  val consumerKey = configuration.getString("twitter.consumer.key").get
  val consumerSecret = configuration.getString("twitter.consumer.secret").get

  val callbackUrl = rootUrl + routes.TwitterLoginController.callback(None, None)
  val TwitterRequestTokenSessionKey: String = "twitter-request-token"

  // Redirect the user to Twitter to begin a TWitter OAuth journey. Twitter will redirect the user back to our callback page below if all goes well.
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

  // The user has returned from a Twitter OAuth journey. We should now have a Twitter request token which we can use to identifiy the Twitter user
  def callback(oauth_token: Option[String], oauth_verifier: Option[String]) = Action.async { request =>
    Logger.info("Received Twitter callback: " + oauth_token + ", " + oauth_verifier)

    val twitterAccessToken: Option[AccessToken] = oauth_token.flatMap { t =>
      oauth_verifier.flatMap { v =>

        request.session.get(TwitterRequestTokenSessionKey).flatMap { srt =>
          val rtMap = Json.parse(srt).as[Map[String, String]]
          val requestToken = new RequestToken(rtMap.get("token").getOrElse(""), rtMap.get("secret").getOrElse(""))
          try {
            Logger.info("Exchanging Twitter request token for access token: " + requestToken + ", " + v)
            val accessToken: AccessToken = getTwitterApi.getOAuthAccessToken(requestToken, v)
            Logger.info("Got Twitter access token: " + accessToken.getToken)
            Some(accessToken)

          } catch {
            case te: TwitterException => {
              Logger.warn("Twitter exception while trying to exchange for access token", te)
              None
            }
            case e: Exception => {
              Logger.error("Exception while trying to exchange for access token", e)
              None
            }
          }
        }
      }
    }

    val withClearedRequestToken: Session = request.session - (TwitterRequestTokenSessionKey)

    twitterAccessToken.fold {
      Logger.info("Twitter callback did not result in a valid Twitter access token; abort the sign in flow")
      Future.successful(Redirect(routes.IndexController.index(None, None)).withSession(withClearedRequestToken))
    } { tat =>

      ugcService.tokenTwitter(tat.getToken, tat.getTokenSecret).map { to =>
        to.fold({ e =>
          val withErrors = withClearedRequestToken +("error", e)
          Redirect(routes.IndexController.index(None, None)).withSession(withClearedRequestToken)

        }, { t =>
          Logger.info("Setting session token: " + t)
          Redirect(routes.IndexController.index(None, None)).withSession(signedInUserService.setSignedInUserOnSession(withClearedRequestToken, t))
        }
        )
      }
    }

  }

  private def getTwitterApi: Twitter = {
    def buildConfig:  twitter4j.conf.Configuration = {
      new  twitter4j.conf.ConfigurationBuilder().setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).build
    }
    new TwitterFactory(buildConfig).getInstance
  }

}
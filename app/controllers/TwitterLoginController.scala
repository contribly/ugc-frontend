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

  // Redirect the user to Twitter to begin a Twitter OAuth journey. Twitter will redirect the user back to our callback page below if all goes well.
  def redirect() = Action.async { request =>
    // Call Twitter for a new request token, store it on our users local session before redirecting the user to Twitter

    val requestToken: RequestToken = getTwitterApi.getOAuthRequestToken(callbackUrl)

    val withRequestToken: Session = request.session + (TwitterRequestTokenSessionKey, serializeTwitterRequestToken(requestToken))

    val loginDialogUrlString = requestToken.getAuthenticationURL
    Logger.info("Redirecting to Twitter login dialog: " + loginDialogUrlString)
    Future.successful(Redirect(loginDialogUrlString).withSession(withRequestToken))
  }

  // The user has returned from a Twitter OAuth journey. We should now have a Twitter request token which we can use to identifiy the Twitter user
  def callback(oauth_token: Option[String], oauth_verifier: Option[String]) = Action.async { request =>
    Logger.info("Received Twitter callback: " + oauth_token + ", " + oauth_verifier)

    val twitterRequestTokenFromSession: Option[String] = request.session.get(TwitterRequestTokenSessionKey)
    val sessionWithClearedRequestToken: Session = request.session - (TwitterRequestTokenSessionKey)

    val twitterAccessToken: Option[AccessToken] = oauth_token.flatMap { t =>
      oauth_verifier.flatMap { v =>

        twitterRequestTokenFromSession.flatMap { srt =>
          deserializeRequestToken(srt).flatMap { rt =>
            try {
              Logger.info("Exchanging Twitter request token for access token: " + rt + ", " + v)
              val accessToken: AccessToken = getTwitterApi.getOAuthAccessToken(rt, v)
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
    }

    twitterAccessToken.fold {
      val withErrors = sessionWithClearedRequestToken + ("error", "Could not obtain a Twitter access token")
      Future.successful(Redirect(routes.LoginController.prompt()).withSession(withErrors))

    } { tat =>

      // We now have a Twitter access token. We now need to exchange it for one of our own API's access tokens using the Twitter grant type.
      ugcService.tokenTwitter(tat.getToken, tat.getTokenSecret).map { to =>
        to.fold({ e =>
          val withErrors = sessionWithClearedRequestToken + ("error", e)
          Redirect(routes.LoginController.prompt()).withSession(withErrors)

        }, { t =>
          Logger.info("Twitter access token succcessfully exchanged for API token: " + t)
          // Call the API verify method to obtain the API user associated with this token and attach them to the local session; this user can now be considered to be signed in.
          Redirect(routes.IndexController.index(None, None)).withSession(signedInUserService.setSignedInUserOnSession(sessionWithClearedRequestToken, t))
        }
        )
      }
    }

  }

  private def getTwitterApi: Twitter = {
    def buildConfig:  twitter4j.conf.Configuration = {
      new twitter4j.conf.ConfigurationBuilder().setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).build
    }
    new TwitterFactory(buildConfig).getInstance
  }

  private def serializeTwitterRequestToken(requestToken: RequestToken): String = {
    Json.toJson(
      Map(
        "token" -> requestToken.getToken,
        "secret" -> requestToken.getTokenSecret
      )
    ).toString()
  }

  private def deserializeRequestToken(json: String): Option[RequestToken] = {
    val rtMap = Json.parse(json).as[Map[String, String]]
    rtMap.get("token").flatMap { t =>
      rtMap.get("secret").map { s =>
        new RequestToken(t, s)
      }
    }
  }

}
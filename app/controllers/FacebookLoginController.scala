package controllers

import com.restfb.FacebookClient.AccessToken
import com.restfb.scope.{ExtendedPermissions, ScopeBuilder}
import com.restfb.{DefaultFacebookClient, FacebookClient, Version}
import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.{Logger, Play}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FacebookLoginController extends Controller {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val rootUrl: String
  val appId: String
  val appSecret: String

  val redirectUrl = rootUrl + routes.FacebookLoginController.callback(None, None, None, None)

  def redirect() = Action.async { request =>
    val scopeBuilder: ScopeBuilder  = new ScopeBuilder()
    scopeBuilder.addPermission(ExtendedPermissions.EMAIL)

    val client: FacebookClient  = new DefaultFacebookClient(Version.VERSION_2_5)
    val loginDialogUrlString = client.getLoginDialogUrl(appId, redirectUrl, scopeBuilder)

    Logger.info("Redirecting to Facebook login dialog: " + loginDialogUrlString)
    Future.successful(Redirect(loginDialogUrlString))
  }


  def callback(code: Option[String], error: Option[String], error_reason: Option[String], error_description: Option[String]) = Action.async { request => // TODO use state CRSF parameter

    code.fold {
      Logger.warn("Expected code parameters was not seen on Facebook login callback")
      error.map { e =>
        Logger.error("Error was: " + e + ", " + error_reason + ", " + error_description)
      }
      Future.successful(Redirect(routes.Application.index(None, None)))

    }{ c =>
      Logger.info("Exchanging Facebook verification code for an access token: " + code)
      val client: FacebookClient = new DefaultFacebookClient(Version.VERSION_2_5)

      val facebookAccessToken: AccessToken = client.obtainUserAccessToken(appId, appSecret, redirectUrl, c) // TODO exception handling
      Logger.info("Obtained user access token: " + facebookAccessToken)

      ugcService.tokenFacebook(facebookAccessToken.getAccessToken).map { to =>
        to.fold {
        Redirect(routes.LoginController.prompt) // TODO user notification of error

      }{ t =>
          Logger.info("Setting session token: " + t)
          Redirect(routes.Application.index(None, None)).withSession(SignedInUserService.sessionTokenKey -> t)
        }
      }

      Future.successful(Redirect(routes.Application.index(None, None)))
    }

  }

}

object FacebookLoginController extends FacebookLoginController {
  override lazy val rootUrl = Play.configuration.getString("root.url").get
  override lazy val appId = Play.configuration.getString("facebook.app.id").get
  override lazy val appSecret = Play.configuration.getString("facebook.app.secret").get
}

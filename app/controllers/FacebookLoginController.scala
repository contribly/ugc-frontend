package controllers

import javax.inject.Inject

import com.restfb.FacebookClient.AccessToken
import com.restfb.scope.{UserDataPermissions, ExtendedPermissions, ScopeBuilder}
import com.restfb.{DefaultFacebookClient, FacebookClient, Version}
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FacebookLoginController @Inject() (configuration: Configuration, ugcService: UGCService, signedInUserService: SignedInUserService) extends Controller {

  val rootUrl = configuration.getString("root.url").get
  val appId = configuration.getString("facebook.app.id").get
  val appSecret =configuration.getString("facebook.app.secret").get

  val callbackUrl = rootUrl + routes.FacebookLoginController.callback(None, None, None, None)

  def redirect() = Action.async { request =>
    val scopeBuilder: ScopeBuilder = new ScopeBuilder().addPermission(ExtendedPermissions.EMAIL).addPermission(UserDataPermissions.USER_ABOUT_ME);
    val client: FacebookClient  = new DefaultFacebookClient(Version.VERSION_2_5)
    val loginDialogUrlString = client.getLoginDialogUrl(appId, callbackUrl, scopeBuilder)

    Logger.info("Redirecting to Facebook login dialog: " + loginDialogUrlString)
    Future.successful(Redirect(loginDialogUrlString))
  }

  def callback(code: Option[String], error: Option[String], error_reason: Option[String], error_description: Option[String]) = Action.async { request => // TODO use state CRSF parameter

    code.fold {
      Logger.warn("Expected code parameters was not seen on Facebook login callback")
      error.map { e =>
        Logger.error("Error was: " + e + ", " + error_reason + ", " + error_description)
      }
      Future.successful(Redirect(routes.LoginController.prompt()))

    } { c =>
      Logger.info("Exchanging Facebook verification code for an access token: " + code)
      val client: FacebookClient = new DefaultFacebookClient(Version.VERSION_2_5)
      val facebookAccessToken: AccessToken = client.obtainUserAccessToken(appId, appSecret, callbackUrl, c) // TODO exception handling
      Logger.info("Obtained user access token: " + facebookAccessToken)

      ugcService.tokenFacebook(facebookAccessToken.getAccessToken).map { to =>
        to.fold(
          { e =>
            val withErrors = request.session +("error", e)
            Redirect(routes.LoginController.prompt).withSession(withErrors)
          }, { t =>
            Logger.info("Setting session token: " + t)
            Redirect(routes.Application.index(None, None)).withSession(signedInUserService.setSignedInUserOnSession(request.session, t))
          }
        )
      }
    }

  }

}
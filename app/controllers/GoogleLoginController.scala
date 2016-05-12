package controllers

import com.netaporter.uri.dsl._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{Result, Action, Controller}
import play.api.{Logger, Play}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GoogleLoginController extends Controller {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val rootUrl: String
  val clientId: String
  val clientSecret: String

  val callbackUrl = rootUrl + routes.GoogleLoginController.callback(None, None)

  def redirect() = Action.async { request =>
    val oauthParams = Seq(
      ("response_type" -> "code"),
      ("client_id" -> clientId),
      ("scope" -> "email"),
      ("redirect_uri" -> callbackUrl)
    )

    val loginDialogUrl= "https://accounts.google.com/o/oauth2/v2/auth".addParams(oauthParams)

    Logger.info("Redirecting to Google login dialog: " + loginDialogUrl)
    Future.successful(Redirect(loginDialogUrl))
  }

  def callback(code: Option[String], error: Option[String]) = Action.async { request =>
    code.fold {
      Logger.warn("Not code parameter seen on callback. error parameter was: " + error)
      Future.successful(Redirect(routes.LoginController.prompt()))

    } { c =>
      Logger.info("Received code from oauth callback: " + code)

      val tokenParams = Map(
        "code" -> Seq(c),
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "redirect_uri" -> Seq(callbackUrl),
        "grant_type" -> Seq("authorization_code")
      )

      val tokenUrl = "https://www.googleapis.com/oauth2/v4/token"

      Logger.info("Exchanging code for token: " + tokenUrl)
      WS.url(tokenUrl).post(tokenParams).flatMap { r => {
        Logger.info("Token response: " + r.body)

        implicit val tr = Json.reads[TokenResponse]
        val token = Json.parse(r.body).as[TokenResponse]
        Logger.info("Got token: " + token)

        ugcService.tokenGoogle(token.access_token).map { to =>
          to.fold({ e =>
            val withErrors = request.session +("error", e)
            Redirect(routes.LoginController.prompt).withSession(withErrors)
          }, { t =>
            Logger.info("Setting session token: " + t)
            Redirect(routes.Application.index(None, None)).withSession(SignedInUserService.sessionTokenKey -> t)
          }
          )
        }
      }
      }

    }

  }

  private case class TokenResponse(access_token: String)

}

object GoogleLoginController extends GoogleLoginController {
  override lazy val rootUrl = Play.configuration.getString("root.url").get
  override lazy val clientId = Play.configuration.getString("google.client.id").get
  override lazy val clientSecret = Play.configuration.getString("google.client.secret").get
}

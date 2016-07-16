package controllers

import javax.inject.Inject

import model.{LoginDetails, User}
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Controller, Request, _}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with WithOwner with I18nSupport {

  val loginForm: Form[LoginDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginDetails.apply)(LoginDetails.unapply)
  )

  def prompt() = Action.async { implicit request =>

    withOwner { owner =>
      val withErrors = request.session.get("error").fold(loginForm) { e =>
        loginForm.withGlobalError(e)
      }
      Future.successful(Ok(views.html.login(withErrors, owner)).withSession(request.session - "error"))
    }
  }

  def submit() = Action.async { implicit request =>

    withOwner { owner =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(Ok(views.html.login(formWithErrors, owner)))
        },
        loginDetails => {
          ugcService.token(loginDetails.username, loginDetails.password).map { to =>
            to.fold({ e =>
              val withErrors = request.session +("error", e)
              Redirect(routes.LoginController.prompt()).withSession(withErrors)
            }, { t =>
              Logger.info("Setting session token: " + t)
              Redirect(routes.Application.index(None, None)).withSession(signedInUserService.setSignedInUserOnSession(request.session, t))
            }
            )
          }
        }
      )
    }
  }

  def logout = Action {
    Redirect(routes.Application.index(None, None)).withNewSession
  }

}
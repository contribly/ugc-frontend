package controllers

import model.{User, LoginDetails}
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Result, Request, Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LoginController extends Controller with WithOwner {

  val signedInUserService = SignedInUserService
  val ugcService = UGCService

  val loginForm: Form[LoginDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginDetails.apply)(LoginDetails.unapply)
  )

  def prompt() = Action.async { request =>

    val loginPromptPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {
      val withErrors = request.session.get("error").fold(loginForm) { e =>
        loginForm.withGlobalError(e)
      }
      Future.successful(Ok(views.html.login(withErrors, owner)).withSession(request.session - "error"))
    }

    withOwner(request, loginPromptPage)
  }

  def submit() = Action.async { request =>

    val loginSubmit: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      loginForm.bindFromRequest()(request).fold(
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
              Redirect(routes.Application.index(None, None)).withSession(SignedInUserService.sessionTokenKey -> t)
            }
            )
          }
        }
      )
    }

    withOwner(request, loginSubmit)
  }

  def logout = Action {
    Redirect(routes.Application.index(None, None)).withNewSession
  }

}
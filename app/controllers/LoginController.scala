
package controllers

import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LoginController extends Controller with PageSize {

  val signedInUserService = SignedInUserService

  val loginForm: Form[LoginDetails] = Form(
    map(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    ).
  )

  case class LoginDetails(username: String, password: String)

  def prompt() = Action.async {
    Future.successful(Ok(views.html.login(loginForm)))
  }

  def submit() = Action.async { request =>

    val boundForm: Form[LoginDetails] = loginForm.bindFromRequest()(request)

    boundForm.fold(
      formWithErrors => {
        Future.successful(Ok(views.html.login(formWithErrors)))
      },
      loginDetails => {
        val eventualMaybeToken: Future[Option[String]] = signedInUserService.signin(loginDetails.username, loginDetails.password, request)
        eventualMaybeToken.map(to => {
          to.fold(
            Ok(views.html.login(loginForm.withGlobalError("Invalid credentials")))
          )(t => {
            Logger.info("Setting session token: " + t)
            Redirect(routes.Application.index()).withSession(SignedInUserService.sessionTokenKey -> t)
          })
        })
      }
    )
  }

  def logout = Action {
    Redirect(routes.Application.index()).withNewSession
  }

}
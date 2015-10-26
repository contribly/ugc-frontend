
package controllers

import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.validation.Constraints._

object LoginController extends Controller with PageSize {

  val signedInUserService = SignedInUserService

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  def prompt() = Action.async {
    Future.successful(Ok(views.html.login(loginForm)))
  }

  def submit() = Action.async { request =>

    val boundForm: Form[(String, String)] = loginForm.bindFromRequest()(request)

    boundForm.fold(
      formWithErrors => {
        Future.successful(BadRequest(Json.toJson("Bad form data: " + formWithErrors.errors)))
      },
      userData => {
        val eventualMaybeToken: Future[Option[String]] = signedInUserService.signin(userData._1, userData._2, request)
        eventualMaybeToken.map(to => {
          to.fold(
            BadRequest(Json.toJson("No token"))
          )(t => {
            Logger.info("Setting session token: " + t)
            Redirect(routes.Application.index()).withSession("token" -> t)
          })
        })
      }
    )
  }

  def logout = Action {
    Redirect(routes.Application.index()).withNewSession
  }

}
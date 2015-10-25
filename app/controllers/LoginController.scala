
package controllers

import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LoginController extends Controller with PageSize {

  val signedInUserService = SignedInUserService

  def login(username: String, password: String) = Action.async {request =>
    val token: Future[Option[String]] = signedInUserService.signin(username, password, request)

    token.map(to => {
      to.fold(Redirect(routes.Application.index()))(t =>
        Redirect(routes.Application.index()).withSession("token" -> t)
      )
    })
  }

  def logout = Action {
    Redirect(routes.Application.index()).withNewSession
  }

}
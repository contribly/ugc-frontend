
package controllers

import java.util.UUID

import play.api.Logger
import play.api.mvc.{Session, Action, Controller}

object LoginController extends Controller with PageSize {

  def login = Action {
    Logger.info("Login called")
    Redirect(routes.Application.index()).withSession("signedin" -> UUID.randomUUID().toString)
  }

  def logout = Action {
    Logger.info("Logout called")
    Redirect(routes.Application.index()).withNewSession
  }

}
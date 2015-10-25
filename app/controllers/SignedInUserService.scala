package controllers

import model.User
import play.api.Logger
import play.api.mvc.{Session, AnyContent, Request}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

class SignedInUserService {

  val sessionTokenKey: String = "token"

  val ugcService = UGCService

  def signin(username: String, password: String, request: Request[AnyContent]): Future[Option[String]] = {
    Logger.info("Attempting to set signed in user token")
    ugcService.token(username, password).map(to => {
      to.map(t => {
        Logger.info("Got token: " + t)
        t
      })
    })
  }

  def signedIn(request: Request[AnyContent]): Future[Option[User]] = {
    val token: Option[String] = request.session.get(sessionTokenKey)
    Logger.info("Token on request: " + token)

    val noneUser: Future[Option[User]] = Future.successful(None)
    token.fold(noneUser)(t => {
      ugcService.verify(t)
    })
  }

}

object SignedInUserService extends SignedInUserService
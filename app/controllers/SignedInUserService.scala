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

  def signedIn(request: Request[Any]): Future[Option[User]] = {
    val token: Option[String] = request.session.get(sessionTokenKey)
    Logger.info("Token on request: " + token)

    val noneUser: Future[Option[User]] = Future.successful(None)
    token.fold(noneUser)(t => {
      ugcService.verify(t)
    })
  }

}

object SignedInUserService extends SignedInUserService
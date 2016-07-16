package controllers

import javax.inject.Inject

import model.User
import play.api.mvc.{Request, Session}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

class SignedInUserService @Inject() (ugcService: UGCService) {

  private val SessionTokenKey: String = "token"

  def setSignedInUserOnSession(session: Session, token: String) = {
    session + (SessionTokenKey, token)
  }

  def signedIn(implicit request: Request[Any]): Future[Option[(User, String)]] = {
    val noneUser: Future[Option[(User, String)]] = Future.successful(None)
    request.session.get(SessionTokenKey).fold(noneUser) { t =>
      ugcService.verify(t).map(ao => ao.flatMap(a => a.user.map(u => (u, t))))
    }
  }

}
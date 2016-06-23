package controllers

import model.User
import play.api.Logger
import play.api.mvc.{Session, Request}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

class SignedInUserService {

  private val sessionTokenKey: String = "token"

  val ugcService = UGCService

  def setSignedInUserOnSession(session: Session, token: String) = {
    session + (sessionTokenKey, token)
  }

  def signedIn(request: Request[Any]): Future[Option[(User, String)]] = {
    val noneUser: Future[Option[(User, String)]] = Future.successful(None)
    request.session.get(sessionTokenKey).fold(noneUser){ t =>
      ugcService.verify(t).map { uo =>
        uo.map { u =>
          (u, t)
        }
      }
    }
  }

}

object SignedInUserService extends SignedInUserService {
}
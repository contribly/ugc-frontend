package controllers

import model.User
import play.api.Logger
import play.api.mvc.Results.NotFound
import play.api.mvc.{Request, Result}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WithOwner {

  def ugcService: UGCService

  def withOwner[T](handlerFunction: (Request[T], User) => Future[Result])(implicit request: Request[T]): Future[Result] = {
    ugcService.owner.flatMap { oo =>
      oo.fold {
        Logger.warn("Owner not found; returning 404")
        Future.successful(NotFound(views.html.notFound()))
      } { o =>
        handlerFunction(request, o)
      }
    }
  }

}

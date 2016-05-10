package controllers

import controllers.Application._
import model.User
import play.api.Logger
import play.api.mvc.{Result, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait WithOwner {

  def withOwner[T](request: Request[T], handlerFunction: (Request[T], User) => Future[Result]): Future[Result] = {
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

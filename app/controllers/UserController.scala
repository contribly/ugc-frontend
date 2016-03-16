package controllers

import model.User
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def user(id: String, page: Option[Int]) = Action.async { request =>

    def pageLinksFor(user: User, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.UserController.user(user.id, Some(p)).url))
    }

    for {
      user <- ugcService.user(id)
      owner <- ugcService.owner
      reports <- ugcService.reports(PageSize, 1, None, None, Some(id), None, None)
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      user.fold(NotFound(views.html.notFound())) { u =>
        owner.fold(NotFound(views.html.notFound())) { o =>
          Ok(views.html.user(u, o, signedIn, reports.results, pageLinksFor(u, reports.numberFound)))
        }
      }
    }
  }

  def profile = Action.async { request =>

    ugcService.owner.flatMap { owner =>
      owner.fold(Future.successful(NotFound(views.html.notFound()))) { o =>
        signedInUserService.signedIn(request).flatMap { signedIn =>
          ugcService.reports(PageSize, 1, None, None, Some(signedIn.get.id), None, request.session.get("token")).map { reports =>
            Ok(views.html.profile(signedIn.get, o, signedIn, reports.results))
          }
        }
      }
    }

  }

}
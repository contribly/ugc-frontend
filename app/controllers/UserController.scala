package controllers

import model.User
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

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
      Ok(views.html.user(user, owner, signedIn, reports.results, pageLinksFor(user, reports.numberFound)))
    }
  }

  def profile = Action.async { request =>

    ugcService.owner.flatMap { owner =>
      signedInUserService.signedIn(request).flatMap { signedIn =>
        ugcService.reports(PageSize, 1, None, None, Some(signedIn.get.id), None, request.session.get("token")).map { reports =>
          Ok(views.html.profile(signedIn.get, owner, signedIn, reports.results))
        }
      }
    }

  }

}
package controllers

import model.User
import play.api.mvc.{Result, Request, Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UserController extends Controller with Pages with WithOwner {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def user(id: String, page: Option[Int]) = Action.async { request =>

    val userPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pageLinksFor(user: User, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.UserController.user(user.id, Some(p)).url))
      }

      for {
        user <- ugcService.user(id)
        reports <- ugcService.reports(PageSize, 1, None, None, Some(id), None, None)
        signedIn <- signedInUserService.signedIn(request)

      } yield {
        user.fold(NotFound(views.html.notFound())) { u =>
          Ok(views.html.user(u, owner, signedIn, reports.results, pageLinksFor(u, reports.numberFound)))
        }
      }
    }

    withOwner(request, userPage)
  }

  def profile = Action.async { request =>

    val profilePage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {
      signedInUserService.signedIn(request).flatMap { so =>
        so.fold{
          Future.successful(Redirect(routes.LoginController.prompt()))
        } { signedIn =>
          ugcService.reports(PageSize, 1, None, None, Some(signedIn.id), None, request.session.get("token")).map { reports =>
            Ok(views.html.profile(signedIn, owner, Some(signedIn), reports.results))
          }
        }
      }
    }

    withOwner(request, profilePage)
  }

}
package controllers

import model.User
import play.api.mvc.{Action, Controller, Request, Result}
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
        reports <- ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(id))
        signedIn <- signedInUserService.signedIn(request)

      } yield {
        user.fold(NotFound(views.html.notFound())) { u =>
          Ok(views.html.user(u, owner, signedIn.map(s => s._1), reports.results, pageLinksFor(u, reports.numberFound)))
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

          val eventualReports = ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(signedIn._1.id), token = Some(signedIn._2))

          for {
            reports <- eventualReports

          } yield {
            Ok(views.html.profile(signedIn._1, owner, Some(signedIn._1), reports.results))
          }
        }
      }
    }

    withOwner(request, profilePage)
  }

}
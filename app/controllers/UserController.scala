package controllers

import model.User
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

object UserController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def user(id: String) = Action.async { request =>

    def pageLinksFor(user: User, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.UserController.user(user.id).url)) // TODO page
    }

    val eventualUser = ugcService.user(id)
    val eventualOwner = ugcService.owner
    val eventualReports = ugcService.reports(PageSize, 1, None, None, Some(id), None)

    for {
      user <- eventualUser
      owner <- eventualOwner
      reports <- eventualReports
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.user(user, owner, signedIn, reports.results, pageLinksFor(user, reports.numberFound)))
    }
  }

}
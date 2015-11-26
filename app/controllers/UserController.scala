package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object UserController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def user(id: String) = Action.async { request =>
    val eventualUser = ugcService.user(id)
    val eventualOwner = ugcService.owner
    val eventualReports = ugcService.reports(PageSize, 1, None, None, Some(id), None)

    for {
      user <- eventualUser
      owner <- eventualOwner
      reports <- eventualReports
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.user(user, owner, signedIn, reports.results, pagesFor(reports.numberFound)))
    }
  }

}
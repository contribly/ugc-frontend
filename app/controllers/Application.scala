
package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with PageSize {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def index(page: Option[Int]) = Action.async {request =>

    val eventualTags = ugcService.tags()
    val eventualReports = ugcService.reports(pageSize, page.fold(1)(p => p), None, None, None, None)
    val eventualOwner = ugcService.owner

    for {
      tags <- eventualTags
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.index(tags, reports.results, owner, signedIn))
    }
  }

  def report(id: String) = Action.async { request =>
    val eventualReport = ugcService.report(id)
    val eventualOwner = ugcService.owner

    for {
      report <- eventualReport
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.report(report, owner, signedIn))
    }
  }

}

package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with PageSize {

  val ugcService = UGCService

  def index = Action.async {request =>

    val signedIn: Option[String] = request.session.get("signedin")
    Logger.info("Signed in: " + signedIn)

    val eventualTags = ugcService.tags()
    val eventualReports = ugcService.reports(pageSize, 1, None)
    val eventualOwner = ugcService.owner

    for {
      tags <- eventualTags
      reports <- eventualReports
      owner <- eventualOwner

    } yield {
      Ok(views.html.index(tags, reports.results, owner, signedIn))
    }
  }

  def noticeboard(id: String) = Action.async {
    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(pageSize, 1, None)

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
    } yield {
      Ok(views.html.noticeboard(noticeboard, reports.results))
    }
  }

  def report(id: String) = Action.async {
    val eventualReport = ugcService.report(id)
    eventualReport.map(r => Ok(views.html.report(r)))
  }

}
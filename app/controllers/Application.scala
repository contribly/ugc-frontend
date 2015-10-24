
package controllers

import model.Tag
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {

  val pageSize: Int = 20

  val ugcService = UGCService

  def index = Action.async {
    val eventualTags: Future[Seq[Tag]] = ugcService.tags()
    val eventualReports = ugcService.reports(pageSize, 1, None)

    for {
      tags <- eventualTags
      reports <- eventualReports
    } yield {
      Ok(views.html.index(tags, reports.results))
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

  def tag(id: String) = Action.async {
    val eventualTag = ugcService.tag(id)
    val eventualReports = ugcService.reports(pageSize, 1, Some(id))

    for {
      tag <- eventualTag
      reports <- eventualReports
    } yield {
      Ok(views.html.tag(tag, reports.results))
    }
  }

}
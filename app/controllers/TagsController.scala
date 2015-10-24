
package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object TagsController extends Controller with PageSize {

  val ugcService = UGCService

  def tags = Action.async {
    val eventualTags = ugcService.tags()
    for {
      tags <- eventualTags
    } yield {
      Ok(views.html.tags(tags))
    }
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
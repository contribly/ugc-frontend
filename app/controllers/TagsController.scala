package controllers

import model.Tag
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

object TagsController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def tags = Action.async { request =>

    val eventualTags = ugcService.tags()
    val eventualOwner = ugcService.owner

    for {
      tags <- eventualTags
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.tags(tags, owner, signedIn))
    }
  }

  def tag(id: String, page: Option[Int]) = Action.async { request =>

    def pageLinksFor(tag: Tag, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.TagsController.tag(tag.id, Some(p)).url))
    }

    val eventualTag = ugcService.tag(id)
    val eventualReports = ugcService.reports(PageSize, 1, Some(id), None, None, None, None)
    val eventualOwner = ugcService.owner
    val eventualTags = ugcService.tags()

    for {
      tag <- eventualTag
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)
      tags <- eventualTags

    } yield {
      Ok(views.html.tag(tag, reports.results, owner, signedIn, tags, pageLinksFor(tag, reports.numberFound)))
    }
  }

}
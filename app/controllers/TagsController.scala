package controllers

import javax.inject.Inject

import model.Tag
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class TagsController @Inject() (ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with I18nSupport {

  def tags = Action.async { implicit request =>

    val eventualTags = ugcService.tags()
    val eventualOwner = ugcService.owner

    for {
      tags <- eventualTags
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.tags(tags, o, signedIn.map(s => s._1)))
      }
    }
  }

  def tag(id: String, page: Option[Int]) = Action.async { implicit request =>

    def pageLinksFor(tag: Tag, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.TagsController.tag(tag.id, Some(p)).url))
    }

    val eventualTag = ugcService.tag(id)
    val eventualReports = ugcService.reports(pageSize = PageSize, page = page, tag = Some(id))
    val eventualOwner = ugcService.owner
    val eventualTags = ugcService.tags()

    for {
      tag <- eventualTag
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)
      tags <- eventualTags

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.tag(tag, reports.results, o, signedIn.map(s => s._1), tags, pageLinksFor(tag, reports.numberFound)))
      }
    }
  }

}
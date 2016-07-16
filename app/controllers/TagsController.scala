package controllers

import javax.inject.Inject

import model.Tag
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TagsController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def tags = Action.async { implicit request =>
    withOwner { owner =>
      for {
        tags <- ugcService.tags()
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.tags(tags, owner, signedIn.map(s => s._1)))
      }
    }
  }

  def tag(id: String, page: Option[Int]) = Action.async { implicit request =>
    withOwner { owner =>

      def pageLinksFor(tag: Tag, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.TagsController.tag(tag.id, Some(p)).url))
      }

      val eventualTag = ugcService.tag(id)
      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, tag = Some(id))
      val eventualTags = ugcService.tags()

      for {
        tag <- eventualTag
        reports <- eventualReports
        signedIn <- signedInUserService.signedIn
        tags <- eventualTags

      } yield {
        Ok(views.html.tag(tag, reports.results, owner, signedIn.map(s => s._1), tags, pageLinksFor(tag, reports.numberFound)))

      }
    }
  }

}
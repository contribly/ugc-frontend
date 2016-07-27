package controllers

import javax.inject.Inject

import model.Tag
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

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

      for {
        tag <- ugcService.tag(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, tag = Some(id))
        signedIn <- signedInUserService.signedIn
        tags <- ugcService.tags()

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.tag(tag, cs.results, owner, signedIn.map(s => s._1), tags, pageLinksFor(tag, cs.numberFound)))
        }
      }
    }
  }

}
package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class IndexController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def index(page: Option[Int], mediaType: Option[String]) = Action.async { implicit request =>

    withOwner { owner =>

      def pagesLinkFor(totalNumber: Long, mediaTypes: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.IndexController.index(Some(p), mediaTypes).url))
      }

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, mediaType = mediaType)
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.index(cs.results, owner, signedIn.map(s => s._1), cs.numberFound, pagesLinkFor(cs.numberFound, mediaType)))
        }
      }
    }
  }

  def gallery(page: Option[Int]) = Action.async { implicit request =>

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.IndexController.gallery(Some(p)).url))
      }

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, mediaType = Some("image"))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.gallery(cs.results, owner, signedIn.map(s => s._1), pageLinksFor(cs.numberFound), cs.numberFound))
        }
      }
    }
  }

  def videos(page: Option[Int]) = Action.async { implicit request =>

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.IndexController.videos(Some(p)).url))
      }

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, mediaType = Some("video"))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.gallery(cs.results, owner, signedIn.map(s => s._1), pageLinksFor(cs.numberFound), cs.numberFound))
        }
      }
    }

  }

}
package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class IndexController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def index(page: Option[Int]) = Action.async { implicit request =>

    val MediaType = "mediaType"

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.IndexController.index(Some(p)).url))
      }

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page)
        refinements <- ugcService.contributionRefinements(refinements = Seq(MediaType)) // A refinements call can be used to obtain counts of contributions with image or videos attached
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          val mediaTypeCounts = refinements.flatMap( rs => rs.get(MediaType)).getOrElse(Map())
          Ok(views.html.index(cs.results, owner, signedIn.map(s => s._1), cs.numberFound, pageLinksFor(cs.numberFound), mediaTypeCounts))
        }
      }
    }
  }

  def gallery(page: Option[Int], mediaType: Option[String]) = Action.async { implicit request =>

    val ImagesAndVideos = "image,video"

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long, mediaType: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.IndexController.gallery(Some(p), mediaType).url))
      }

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, mediaType = Some(mediaType.getOrElse(ImagesAndVideos)))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.gallery(cs.results, owner, signedIn.map(s => s._1), pageLinksFor(cs.numberFound, mediaType), cs.numberFound))
        }
      }
    }
  }

}
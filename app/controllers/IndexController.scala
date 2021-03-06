package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class IndexController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  val MediaType = "mediaType"

  def index(page: Option[Long]) = Action.async { implicit request =>

    withOwner { owner =>

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page)
        refinements <- ugcService.contributionRefinements(refinements = Seq(MediaType)) // A refinements call can be used to obtain counts of contributions with image or videos attached
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          val mediaTypeCounts = refinements.flatMap( rs => rs.get(MediaType)).getOrElse(Map())
          val nextPage = nextPageFor(cs.numberFound, page).map(p => PageLink(routes.IndexController.index(Some(p)).url))
          Ok(views.html.index(cs.results, owner, signedIn.map(s => s._1), cs.numberFound, nextPage, mediaTypeCounts))
        }
      }
    }
  }

  def gallery(page: Option[Long], mediaType: Option[String]) = Action.async { implicit request =>

    val ImagesAndVideos = "image,video"

    withOwner { owner =>

      for {
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, mediaType = Some(mediaType.getOrElse(ImagesAndVideos)))
        refinements <- ugcService.contributionRefinements(refinements = Seq(MediaType))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          val nextPage = nextPageFor(cs.numberFound, page).map(p => PageLink(routes.IndexController.gallery(Some(p), mediaType).url))
          val mediaTypeCounts = refinements.flatMap( rs => rs.get(MediaType)).getOrElse(Map())
          Ok(views.html.gallery(cs.results, owner, signedIn.map(s => s._1), nextPage, cs.numberFound, mediaTypeCounts))
        }
      }
    }
  }

}

package controllers

import model.User
import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink
import play.api.mvc.{Request, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def index(page: Option[Int], hasMediaType: Option[String]) = Action.async { request =>

    val indexPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pagesLinkFor(totalNumber: Long, hasMediaType: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.index(Some(p), hasMediaType).url))
      }

      val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, hasMediaType, None)
      val eventualVerifiedSignedInUser = signedInUserService.signedIn(request)

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.index(reports.results, owner, signedIn, reports.numberFound, pagesLinkFor(reports.numberFound, hasMediaType)))
      }
    }

    withOwner(request, indexPage)
  }

  def gallery(page: Option[Int]) = Action.async { request =>

    def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.gallery(Some(p)).url))
    }

    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, Some("image"), None)
    val eventualOwner = ugcService.owner

    for {
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.gallery(reports.results, o, signedIn, pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }
  }

  def videos(page: Option[Int]) = Action.async { request =>

    def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.videos(Some(p)).url))
    }

    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, Some("video"), None)
    val eventualOwner = ugcService.owner

    for {
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.gallery(reports.results, o, signedIn, pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }
  }

  private def withOwner[T](request: Request[T], handlerFunction: (Request[T], User) => Future[Result]): Future[Result] = {
    ugcService.owner.flatMap { oo =>
      oo.fold {
        Logger.warn("Owner not found; returning 404")
        Future.successful(NotFound(views.html.notFound()))
      } { o =>
        handlerFunction(request, o)
      }
    }
  }

}
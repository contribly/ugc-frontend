
package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def index(page: Option[Int], hasMediaType: Option[String]) = Action.async {request =>

    def pagesLinkFor(totalNumber: Long, hasMediaType: Option[String]): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.index(Some(p), hasMediaType).url))
    }

    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, hasMediaType, None)
    val eventualOwner = ugcService.owner

    for {
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.index(reports.results, o, signedIn, reports.numberFound, pagesLinkFor(reports.numberFound, hasMediaType)))
      }
    }
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

}
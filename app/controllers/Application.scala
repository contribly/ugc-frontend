
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

    val eventualTags = ugcService.tags()
    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, hasMediaType)
    val eventualOwner = ugcService.owner

    for {
      tags <- eventualTags
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.index(tags, reports.results, owner, signedIn, reports.numberFound, pagesLinkFor(reports.numberFound, hasMediaType)))
    }
  }

  def gallery(page: Option[Int]) = Action.async { request =>

    def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.gallery(Some(p)).url))
    }

    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, Some("image"))
    val eventualOwner = ugcService.owner

    for {
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.gallery(reports.results, owner, signedIn, pageLinksFor(reports.numberFound)))
    }
  }

  def report(id: String) = Action.async { request =>
    val eventualReport = ugcService.report(id)
    val eventualOwner = ugcService.owner

    for {
      report <- eventualReport
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.report(report, owner, signedIn))
    }
  }

  def videos(page: Option[Int]) = Action.async { request =>

    def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.videos(Some(p)).url))
    }

    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, None, None, Some("video"))
    val eventualOwner = ugcService.owner

    for {
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.gallery(reports.results, owner, signedIn, pageLinksFor(reports.numberFound)))
    }
  }

}
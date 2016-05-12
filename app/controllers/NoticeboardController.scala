package controllers

import model.Noticeboard
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

object NoticeboardController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def noticeboards(page: Option[Int]) = Action.async { request =>

    def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.noticeboards(Some(p)).url))
    }

    val eventualNoticeboards = ugcService.noticeboards(PageSize, page.fold(1)(p => p))
    val eventualOwner = ugcService.owner

    for {
      noticeboards <- eventualNoticeboards
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.noticeboards(noticeboards.results, o, signedIn.map(s => s._1), pagesLinkFor(noticeboards.numberFound.toInt)))
      }
    }
  }

  def noticeboard(id: String, page: Option[Int]) = Action.async { request =>

    def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.noticeboard(noticeboard.id, Some(p)).url))
    }

    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, Some(id), None, None, None)
    val eventualOwner = ugcService.owner

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.noticeboard(noticeboard, reports.results, o, signedIn.map(s => s._1), reports.numberFound, pageLinksFor(noticeboard, reports.numberFound)))
      }
    }
  }

  def gallery(id: String, page: Option[Int]) = Action.async { request =>

    def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.gallery(noticeboard.id, Some(p)).url))
    }

    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, Some(id), None, Some("image"), None)
    val eventualOwner = ugcService.owner

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.noticeboardGallery(noticeboard, reports.results, o, signedIn.map(s => s._1), pageLinksFor(noticeboard, reports.numberFound)))
      }
    }
  }

}
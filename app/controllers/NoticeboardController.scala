package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object NoticeboardController extends Controller with Pages {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def noticeboards(page: Option[Int]) = Action.async { request =>

    val eventualNoticeboards = ugcService.noticeboards(PageSize, page.fold(1)(p => p))
    val eventualOwner = ugcService.owner

    for {
      noticeboards <- eventualNoticeboards
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.noticeboards(noticeboards.results, owner, signedIn, pagesFor(noticeboards.numberFound.toInt)))
    }
  }

  def noticeboard(id: String, page: Option[Int]) = Action.async { request =>
    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, Some(id), None, None)
    val eventualOwner = ugcService.owner

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.noticeboard(noticeboard, reports.results, owner, signedIn, reports.numberFound, pagesFor(reports.numberFound.toInt)))
    }
  }

  def gallery(id: String, page: Option[Int]) = Action.async { request =>
    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(PageSize, page.fold(1)(p => p), None, Some(id), None, Some("image"))
    val eventualOwner = ugcService.owner

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.gallery(noticeboard, reports.results, owner, signedIn))
    }
  }

}
package controllers

import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object NoticeboardController extends Controller with PageSize {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def noticeboards() = Action.async { request =>
    val eventualNoticeboards = ugcService.noticeboards(pageSize, 1)
    val eventualReports = ugcService.reports(pageSize, 1, None, None, None)
    val eventualOwner = ugcService.owner

    for {
      noticeboards <- eventualNoticeboards
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.noticeboards(noticeboards.results, owner, signedIn))
    }
  }

  def noticeboard(id: String) = Action.async { request =>
    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(pageSize, 1, None, Some(id), None)
    val eventualOwner = ugcService.owner

    for {
      noticeboard <- eventualNoticeboard
      reports <- eventualReports
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.noticeboard(noticeboard, reports.results, owner, signedIn))
    }
  }

}
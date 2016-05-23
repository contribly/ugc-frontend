package controllers

import model.{SearchResult, User, Noticeboard}
import play.api.mvc.{Result, Request, Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NoticeboardController extends Controller with Pages with WithOwner {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def noticeboards(page: Option[Int]) = Action.async { request =>

    val noticeboardsPages: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.noticeboards(Some(p)).url))
      }

      val eventualNoticeboards = ugcService.noticeboards(PageSize, page.fold(1)(p => p))
      val eventualNoticeboardContributionCounts = ugcService.reports(pageSize = 0, refinements = Some(Seq("noticeboard")))

      for {
        noticeboards <- eventualNoticeboards
        noticeboardContributionCounts <- eventualNoticeboardContributionCounts
        signedIn <- signedInUserService.signedIn(request)

      } yield {
          val contributionCounts: Map[String, Long] = noticeboardContributionCounts.refinements.get("noticeboard")
          Ok(views.html.noticeboards(noticeboards.results, owner, signedIn.map(s => s._1), pagesLinkFor(noticeboards.numberFound.toInt), contributionCounts))
      }

    }

    withOwner(request, noticeboardsPages)
  }

  def noticeboard(id: String, page: Option[Int]) = Action.async { request =>

    def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
      pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.noticeboard(noticeboard.id, Some(p)).url))
    }

    val eventualNoticeboard = ugcService.noticeboard(id)
    val eventualReports = ugcService.reports(pageSize = PageSize, page, noticeboard = Some(id))
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
    val eventualReports = ugcService.reports(pageSize = PageSize, page = page, noticeboard = Some(id), hasMediaType = Some("image"))
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
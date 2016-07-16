package controllers

import javax.inject.Inject

import model.{Noticeboard, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request, Result}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NoticeboardController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  private val Assignment = "assignment"

  def assignments(page: Option[Int]) = Action.async { request =>

    implicit val implicitRequestNeededForI18N = request  // TODO Suggests that play expects out wrappers to leave the request as an implicit

    val noticeboardsPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.assignments(Some(p)).url))
      }

      val eventualNoticeboards = ugcService.assignments(PageSize, page.fold(1)(p => p))
      val eventualNoticeboardContributionCounts = ugcService.reports(pageSize = 0, refinements = Some(Seq(Assignment)))

      for {
        noticeboards <- eventualNoticeboards
        noticeboardContributionCounts <- eventualNoticeboardContributionCounts
        signedIn <- signedInUserService.signedIn(request)

      } yield {
          val contributionCounts: Map[String, Long] = noticeboardContributionCounts.refinements.get(Assignment)
          Ok(views.html.noticeboards(noticeboards.results, owner, signedIn.map(s => s._1), pagesLinkFor(noticeboards.numberFound.toInt), contributionCounts))
      }
    }

    withOwner(noticeboardsPage, request)
  }

  def assignment(id: String, page: Option[Int]) = Action.async { request =>

    implicit val implicitRequestNeededForI18N = request  // TODO Suggests that play expects out wrappers to leave the request as an implicit

    val noticeboardPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.assignment(noticeboard.id, Some(p)).url))
      }

      val eventualNoticeboard = ugcService.assignment(id)
      val eventualReports = ugcService.reports(pageSize = PageSize, page, assignment = Some(id))

      for {
        noticeboard <- eventualNoticeboard
        reports <- eventualReports
        signedIn <- signedInUserService.signedIn(request)

      } yield {
        Ok(views.html.noticeboard(noticeboard, reports.results, owner, signedIn.map(s => s._1), reports.numberFound, pageLinksFor(noticeboard, reports.numberFound)))
      }
    }

    withOwner(noticeboardPage, request)
  }

  def gallery(id: String, page: Option[Int]) = Action.async { request =>

    implicit val implicitRequestNeededForI18N = request  // TODO Suggests that play expects out wrappers to leave the request as an implicit

    val galleyPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.gallery(noticeboard.id, Some(p)).url))
      }

      val eventualNoticeboard = ugcService.assignment(id)
      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, assignment = Some(id), hasMediaType = Some("image"))

      for {
        noticeboard <- eventualNoticeboard
        reports <- eventualReports
        signedIn <- signedInUserService.signedIn(request)

      } yield {
        Ok(views.html.noticeboardGallery(noticeboard, reports.results, owner, signedIn.map(s => s._1), pageLinksFor(noticeboard, reports.numberFound)))

      }
    }

    withOwner(galleyPage, request)
  }

}
package controllers

import javax.inject.Inject

import model.{Noticeboard, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class NoticeboardController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  private val Assignment = "assignment"

  def assignments(page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.assignments(Some(p)).url))
      }

      for {
        assignments <- ugcService.assignments(PageSize, page.fold(1)(p => p))
        signedIn <- signedInUserService.signedIn

      } yield {
          val contributionCounts: Map[String, Long] = Map()
          Ok(views.html.noticeboards(assignments.results, owner, signedIn.map(s => s._1), pagesLinkFor(assignments.numberFound.toInt), contributionCounts))
      }
    }

    withOwner(assignmentsPage)
  }

  def assignment(id: String, page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.assignment(noticeboard.id, Some(p)).url))
      }

      val eventualNoticeboard = ugcService.assignment(id)
      val eventualReports = ugcService.contributions(pageSize = PageSize, page, assignment = Some(id))

      for {
        noticeboard <- eventualNoticeboard
        reports <- eventualReports
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.noticeboard(noticeboard, reports.results, owner, signedIn.map(s => s._1), reports.numberFound, pageLinksFor(noticeboard, reports.numberFound)))
      }
    }

    withOwner(assignmentsPage)
  }

  def gallery(id: String, page: Option[Int]) = Action.async { implicit request =>

    val galleyPage = (owner: User) => {

      def pageLinksFor(noticeboard: Noticeboard, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.NoticeboardController.gallery(noticeboard.id, Some(p)).url))
      }

      val eventualNoticeboard = ugcService.assignment(id)
      val eventualReports = ugcService.contributions(pageSize = PageSize, page = page, assignment = Some(id), mediaType = Some("image"))

      for {
        noticeboard <- eventualNoticeboard
        reports <- eventualReports
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.noticeboardGallery(noticeboard, reports.results, owner, signedIn.map(s => s._1), pageLinksFor(noticeboard, reports.numberFound)))

      }
    }

    withOwner(galleyPage)
  }

}
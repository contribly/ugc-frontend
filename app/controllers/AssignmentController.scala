package controllers

import javax.inject.Inject

import model.{Assignment, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class AssignmentController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  private val Assignment = "assignment"

  def assignments(page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.assignments(Some(p)).url))
      }

      for {
        assignments <- ugcService.assignments(PageSize, page.fold(1)(p => p))
        signedIn <- signedInUserService.signedIn

      } yield {
          val contributionCounts: Map[String, Long] = Map()
          Ok(views.html.assignments(assignments.results, owner, signedIn.map(s => s._1), pagesLinkFor(assignments.numberFound.toInt), contributionCounts))
      }
    }

    withOwner(assignmentsPage)
  }

  def assignment(id: String, page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      def pageLinksFor(noticeboard: Assignment, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.assignment(noticeboard.id, Some(p)).url))
      }

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page, assignment = Some(id))
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.assignment(assignment, contributions.results, owner, signedIn.map(s => s._1), contributions.numberFound, pageLinksFor(assignment, contributions.numberFound)))
      }
    }

    withOwner(assignmentsPage)
  }

  def gallery(id: String, page: Option[Int]) = Action.async { implicit request =>

    val galleyPage = (owner: User) => {

      def pageLinksFor(noticeboard: Assignment, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.gallery(noticeboard.id, Some(p)).url))
      }

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, assignment = Some(id), mediaType = Some("image"))
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.assignmentGallery(assignment, contributions.results, owner, signedIn.map(s => s._1), pageLinksFor(assignment, contributions.numberFound)))

      }
    }

    withOwner(galleyPage)
  }

}
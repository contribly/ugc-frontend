package controllers

import javax.inject.Inject

import model.{Assignment, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class AssignmentController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def assignments(page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      val AssignmentRefinement = "assignment"

      def pagesLinkFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.assignments(Some(p)).url))
      }

      for {
        assignments <- ugcService.assignments(PageSize, page.fold(1)(p => p))
        contributionRefinements <- ugcService.contributionRefinements(refinements = Seq(AssignmentRefinement))
        signedIn <- signedInUserService.signedIn

      } yield {

          // Decorate the assignments with contribution counts obtained from calling the contribution refinements end point
          val assignmentContributionCounts = contributionRefinements.flatMap(rs => rs.get(AssignmentRefinement)).getOrElse(Map())

          Ok(views.html.assignments(assignments.results, owner, signedIn.map(s => s._1), pagesLinkFor(assignments.numberFound.toInt), assignmentContributionCounts))
      }
    }

    withOwner(assignmentsPage)
  }

  def assignment(id: String, page: Option[Int]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      def pageLinksFor(assignment: Assignment, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.assignment(assignment.id, Some(p)).url))
      }

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page, assignment = Some(id))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.assignment(assignment, cs.results, owner, signedIn.map(s => s._1), cs.numberFound, pageLinksFor(assignment, cs.numberFound)))
        }
      }
    }

    withOwner(assignmentsPage)
  }

  def gallery(id: String, page: Option[Int]) = Action.async { implicit request =>

    val galleyPage = (owner: User) => {

      def pageLinksFor(assignment: Assignment, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.AssignmentController.gallery(assignment.id, Some(p)).url))
      }

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, assignment = Some(id), mediaType = Some("image"))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          Ok(views.html.assignmentGallery(assignment, cs.results, owner, signedIn.map(s => s._1), pageLinksFor(assignment, cs.numberFound)))
        }
      }
    }

    withOwner(galleyPage)
  }

}
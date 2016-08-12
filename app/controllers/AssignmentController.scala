package controllers

import javax.inject.Inject

import model.{Assignment, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class AssignmentController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def assignments() = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      val AssignmentRefinement = "assignment"

      for {
        assignments <- ugcService.assignments(Some(PageSize))
        contributionRefinements <- ugcService.contributionRefinements(refinements = Seq(AssignmentRefinement))
        signedIn <- signedInUserService.signedIn

      } yield {

          // Decorate the assignments with contribution counts obtained from calling the contribution refinements end point
          val assignmentContributionCounts = contributionRefinements.flatMap(rs => rs.get(AssignmentRefinement)).getOrElse(Map())

        Ok(views.html.assignments(assignments.results, owner, signedIn.map(s => s._1), assignmentContributionCounts))
      }
    }

    withOwner(assignmentsPage)
  }

  def assignment(id: String, page: Option[Long]) = Action.async { implicit request =>

    val assignmentsPage = (owner: User) => {

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page, assignment = Some(id))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          val nextPage = nextPageFor(cs.numberFound, page).map(p => PageLink(routes.AssignmentController.assignment(id, Some(p)).url))
          Ok(views.html.assignment(assignment, cs.results, owner, signedIn.map(s => s._1), cs.numberFound, nextPage))
        }
      }
    }

    withOwner(assignmentsPage)
  }

  def gallery(id: String, page: Option[Long]) = Action.async { implicit request =>

    val galleyPage = (owner: User) => {

      for {
        assignment <- ugcService.assignment(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page = page, assignment = Some(id), mediaType = Some("image"))
        signedIn <- signedInUserService.signedIn

      } yield {
        contributions.fold(NotFound(views.html.notFound())) { cs =>
          val nextPage = nextPageFor(cs.numberFound, page).map(p => PageLink(routes.AssignmentController.gallery(id, Some(p)).url))
          Ok(views.html.assignmentGallery(assignment, cs.results, owner, signedIn.map(s => s._1), nextPage))
        }
      }
    }

    withOwner(galleyPage)
  }

}
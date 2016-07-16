package controllers

import javax.inject.Inject

import model.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request, Result}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def user(id: String, page: Option[Int]) = Action.async { implicit request =>

    val userPage = (owner: User) => {

      def pageLinksFor(user: User, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.UserController.user(user.id, Some(p)).url))
      }

      for {
        user <- ugcService.user(id)
        reports <- ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(id))
        signedIn <- signedInUserService.signedIn

      } yield {
        user.fold(NotFound(views.html.notFound())) { u =>
          Ok(views.html.user(u, owner, signedIn.map(s => s._1), reports.results, pageLinksFor(u, reports.numberFound)))
        }
      }
    }

    withOwner(userPage)
  }

  def profile = Action.async { implicit request =>

    val profilePage = (owner: User) => {

      signedInUserService.signedIn.flatMap { so =>
        so.fold{
          Future.successful(Redirect(routes.LoginController.prompt()))
        } { signedIn =>

          val eventualApprovedReports = ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(signedIn._1.id), state = Some("approved"), token = Some(signedIn._2))
          val eventualAwaitingReports = ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(signedIn._1.id), state = Some("awaiting"), token = Some(signedIn._2))
          val eventualRejectedReports = ugcService.reports(pageSize = PageSize, page = Some(1), user = Some(signedIn._1.id), state = Some("rejected"), token = Some(signedIn._2))

          for {
            approved <- eventualApprovedReports
            awaiting <- eventualAwaitingReports
            rejected <- eventualRejectedReports

          } yield {
            Ok(views.html.profile(signedIn._1, owner, Some(signedIn._1), approved, awaiting, rejected))
          }
        }
      }
    }

    withOwner(profilePage)
  }

}
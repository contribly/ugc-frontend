package controllers

import javax.inject.Inject

import model.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserController @Inject()(val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def user(id: String, page: Option[Int]) = Action.async { implicit request =>

    val userPage = (owner: User) => {

      def pageLinksFor(user: User, totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.UserController.user(user.id, Some(p)).url))
      }

      for {
        user <- ugcService.user(id)
        contributions <- ugcService.contributions(pageSize = PageSize, page = Some(1), user = Some(id))
        signedIn <- signedInUserService.signedIn

      } yield {
        user.fold(NotFound(views.html.notFound())) { u =>
          contributions.fold(NotFound(views.html.notFound())) { cs =>
            Ok(views.html.user(u, owner, signedIn.map(s => s._1), cs.results, pageLinksFor(u, cs.numberFound)))
          }
        }
      }
    }

    withOwner(userPage)
  }

  def profile = Action.async { implicit request =>

    val profilePage = (owner: User) => {

      signedInUserService.signedIn.flatMap { so =>
        so.fold {
          Future.successful(Redirect(routes.LoginController.prompt()))
        } { s =>

          val signedInUser: User = s._1
          val signedInUsersAccessToken = s._2

          for {
            approved <- ugcService.contributions(pageSize = PageSize, page = Some(1), user = Some(signedInUser.id), state = Some("approved"), token = None) // No authetication is required to see approved public contributions
            awaiting <- ugcService.contributions(pageSize = PageSize, page = Some(1), user = Some(signedInUser.id), state = Some("awaiting"), token = Some(signedInUsersAccessToken)) // The user can view their awaiting contributions by appending their access token to the request
            rejected <- ugcService.contributions(pageSize = PageSize, page = Some(1), user = Some(signedInUser.id), state = Some("rejected"), token = Some(signedInUsersAccessToken)) // The user can view their rejected contributions by appending their access token to the request. This would be unwise to allow in production

          } yield {
            approved.fold(NotFound(views.html.notFound())) { ap =>
              awaiting.fold(NotFound(views.html.notFound())) { aw =>
                rejected.fold(NotFound(views.html.notFound())) { rj =>
                  Ok(views.html.profile(signedInUser, owner, Some(signedInUser), ap, aw, rj))
                }
              }
            }
          }
        }
      }
    }

    withOwner(profilePage)
  }

}
package controllers

import javax.inject.Inject

import model.User
import model.forms.RegistrationDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Request, Result}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegisterController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with WithOwner with I18nSupport {

  val registrationForm: Form[RegistrationDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(RegistrationDetails.apply)(RegistrationDetails.unapply)
  )

  def prompt() = Action.async { implicit request =>

    val registerPromptPage: (Request[Any], User) => Future[Result] = (r: Request[Any], owner: User) => {
      val withErrors = request.session.get("error").fold(registrationForm) { e =>
        registrationForm.withGlobalError(e)
      }
      Future.successful(Ok(views.html.register(withErrors, owner)).withSession(r.session - "error"))
    }

    withOwner(registerPromptPage)
  }

  def submit() = Action.async { implicit request =>

    val registerSubmit: (Request[Any], User) => Future[Result] = (r: Request[Any], owner: User) => {

      registrationForm.bindFromRequest()(r).fold(
        formWithErrors => {
          Future.successful(Ok(views.html.register(formWithErrors, owner)))
        },
        registrationDetails => {
          Logger.info("Attempting to register user: " + registrationDetails)
          ugcService.register(registrationDetails).flatMap { mu =>

            mu.fold({ e =>
              Logger.warn("Failed to register user: " + e)
              val withErrors = r.session +("error", e)
              Future.successful(Redirect(routes.RegisterController.prompt()).withSession(withErrors))

            }, { u =>
              Logger.info("Registered new user: " + u)
              ugcService.token(u.username, registrationDetails.password).map { to => // TODO Register end point should provide a token as well
                to.fold ({ e =>
                  val withErrors = r.session +("error", e)
                  Redirect(routes.Application.index(None, None)).withSession(withErrors)

                }, { t =>
                  Logger.info("Got token: " + t)
                  Redirect(routes.Application.index(None, None)).withSession(signedInUserService.setSignedInUserOnSession(r.session, t))
                }
                )
              }
            }
            )
          }
        }
      )
    }

    withOwner(registerSubmit)
  }

}
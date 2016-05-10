package controllers

import model.forms.RegistrationDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RegisterController extends Controller {

  val signedInUserService = SignedInUserService
  val ugcService = UGCService

  val registrationForm: Form[RegistrationDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(RegistrationDetails.apply)(RegistrationDetails.unapply)
  )

  def prompt() = Action.async {
    for {
      owner <- ugcService.owner
    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.register(registrationForm, o))
      }
    }
  }

  def submit() = Action.async { request =>

    val eventualOwner = ugcService.owner

    eventualOwner.flatMap { owner =>

      owner.fold {
        Logger.warn("Invalid owner; returning 404")
        Future.successful(NotFound(views.html.notFound()))
      } { o =>

        registrationForm.bindFromRequest()(request).fold(
          formWithErrors => {
            Future.successful(Ok(views.html.register(formWithErrors, o)))
          },
          registrationDetails => {
            Logger.info("Attempting to register user: " + registrationDetails)
            ugcService.register(registrationDetails).flatMap { mu =>
              mu.fold {
                Logger.warn("Failed to register user") // TODO user feedback
                Future.successful(Ok(views.html.register(registrationForm, o)))
              } { u =>
                Logger.info("Registered : " + u)

                Logger.info("Attempting to set signed in user token") // TODO Register end point should provide a token as well
                ugcService.token(u.username, registrationDetails.password).map { to =>
                  to.fold {
                    Redirect(routes.Application.index(None, None))
                  } { t =>
                    Logger.info("Got token: " + t)
                    Redirect(routes.Application.index(None, None)).withSession(SignedInUserService.sessionTokenKey -> t)
                  }
                }
              }
            }
          }
        )
      }
    }
  }

}
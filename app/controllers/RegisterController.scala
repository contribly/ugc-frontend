package controllers

import model.RegistrationDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

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
    val eventualOwner = ugcService.owner

    for {
      owner <- eventualOwner
    } yield {
      Ok(views.html.register(registrationForm, owner))
    }
  }

  def submit() = Action.async { request =>

    val eventualOwner = ugcService.owner

    eventualOwner.map { owner =>

      val boundForm: Form[RegistrationDetails] = registrationForm.bindFromRequest()(request)

      boundForm.fold(
        formWithErrors => {
          Ok(views.html.register(formWithErrors, owner))
        },
        registrationDetails => {
          Logger.info("Attempting to register user: " + registrationForm)

          ugcService.register(registrationDetails).map { mu =>
            Logger.info("Register user result: " + mu)
          }

          Ok(views.html.register(registrationForm, owner))
        }
      )

    }
  }

}
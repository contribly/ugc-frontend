package controllers

import model.RegistrationDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Result, Action, Controller}
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
    val eventualOwner = ugcService.owner

    for {
      owner <- eventualOwner
    } yield {
      Ok(views.html.register(registrationForm, owner))
    }
  }

  def submit() = Action.async { request =>

    val eventualOwner = ugcService.owner

    eventualOwner.flatMap { owner =>

      val boundForm: Form[RegistrationDetails] = registrationForm.bindFromRequest()(request)

      boundForm.fold(
        formWithErrors => {
          Future.successful(Ok(views.html.register(formWithErrors, owner)))
        },
        registrationDetails => {
          Logger.info("Attempting to register user: " + registrationForm)

          ugcService.register(registrationDetails).map { mu =>
            mu.fold(Ok(views.html.register(registrationForm, owner))) { u =>
              Logger.info("Registered : " + u)
              Ok(views.html.register(registrationForm, owner))
            }
          }
        }
      )

    }
  }

}
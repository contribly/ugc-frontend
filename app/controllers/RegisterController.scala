package controllers

import model.RegistrationDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

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
    Future.successful(Ok(views.html.register(registrationForm)))
  }

  def submit() = Action.async { request =>

    val boundForm: Form[RegistrationDetails] = registrationForm.bindFromRequest()(request)

    boundForm.fold(
      formWithErrors => {
        Future.successful(Ok(views.html.register(formWithErrors)))
      },
      registrationDetails => {
        Logger.info("Attempting to register user: " + registrationForm)

        ugcService.register(registrationDetails)

        Future.successful(Ok(views.html.register(registrationForm)))
      }
    )
  }

}
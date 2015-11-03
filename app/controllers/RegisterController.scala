package controllers

import model.RegistrationDetails
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

object RegisterController extends Controller {

  val signedInUserService = SignedInUserService

  val registrationForm: Form[RegistrationDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(RegistrationDetails.apply)(RegistrationDetails.unapply)
  )

  def prompt() = Action.async {
    Future.successful(Ok(views.html.register(registrationForm)))
  }

  def submit() = Action.async {
    Future.successful(Ok(views.html.register(registrationForm)))
  }

}
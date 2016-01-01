package controllers

import model.LoginDetails
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LoginController extends Controller {

  val signedInUserService = SignedInUserService
  val ugcService = UGCService

  val loginForm: Form[LoginDetails] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginDetails.apply)(LoginDetails.unapply)
  )

  def prompt() = Action.async {

    val eventualOwner = ugcService.owner
    for {
      owner <- eventualOwner
    } yield {
      Ok(views.html.login(loginForm, owner))
    }
  }

  def submit() = Action.async { request =>

    val eventualOwner = ugcService.owner

    eventualOwner.flatMap(owner => {

      val boundForm: Form[LoginDetails] = loginForm.bindFromRequest()(request)
      boundForm.fold(
        formWithErrors => {
          Future.successful(Ok(views.html.login(formWithErrors, owner)))
        },
        loginDetails => {
          val eventualMaybeToken: Future[Option[String]] = signedInUserService.signin(loginDetails.username, loginDetails.password, request)
          eventualMaybeToken.map(to => {
            to.fold(
              Ok(views.html.login(loginForm.withGlobalError("Invalid credentials"), owner))
            )(t => {
              Logger.info("Setting session token: " + t)
              Redirect(routes.Application.index(None, None)).withSession(SignedInUserService.sessionTokenKey -> t)
            })
          })
        }
      )
    })

  }

  def logout = Action {
    Redirect(routes.Application.index(None, None)).withNewSession
  }

}
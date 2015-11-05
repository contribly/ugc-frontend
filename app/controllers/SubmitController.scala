package controllers

import model.{Report, SubmissionDetails}
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubmitController extends Controller {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val submitForm: Form[SubmissionDetails] = Form(
    mapping(
      "headline" -> nonEmptyText,
      "body" -> nonEmptyText
    )(SubmissionDetails.apply)(SubmissionDetails.unapply)
  )

  def prompt() = Action.async { request =>

    val eventualOwner = ugcService.owner
    for {
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.submit(submitForm, owner, signedIn))
    }
  }

  def submit() = Action.async { request =>
    val eventualOwner = ugcService.owner
    eventualOwner.flatMap(owner => {

      val signedIn = signedInUserService.signedIn(request)

      signedIn.flatMap(signedIn => {

        val boundForm: Form[SubmissionDetails] = submitForm.bindFromRequest()(request)
        Logger.info("Bound submission form: " + boundForm)
        boundForm.fold(
          formWithErrors => {
            Logger.info("Form failed to validate: " + formWithErrors)
            Future.successful(Ok(views.html.submit(formWithErrors, owner, signedIn)))
          },
          submissionDetails => {
            Logger.info("Successfully validated submission details: " + submissionDetails)

            val submissionResult: Future[Option[Report]] = ugcService.submit(submissionDetails.headline, submissionDetails.body, request.session.get("token").get)  //TODO should be on the signed in user
            submissionResult.map(or => {
              Logger.info("Submission result: " + or)
              or.fold({
                Logger.info("Redirecting to homepage")
                Redirect(routes.Application.index())
              }
              )(r => {
                Logger.info("Redirecting to report: " + r.id)
                Redirect(routes.Application.report(r.id))
              })
            })
          }
        )
      })

    })
  }

}
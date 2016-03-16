package controllers

import controllers.SubmitController._
import model.forms.FlagSubmission
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Action
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object ReportController {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  val flagForm: Form[FlagSubmission] = Form(
    mapping(
      "type" -> optional(text),
      "notes" -> optional(text)
    )(FlagSubmission.apply)(FlagSubmission.unapply)
  )

  def report(id: String) = Action.async { request =>
    val eventualReport = ugcService.report(id)
    val eventualOwner = ugcService.owner
    val eventualFlagTypes = ugcService.flagTypes

    for {
      report <- eventualReport
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)
      flagTypes <- eventualFlagTypes

    } yield {
      owner.fold(NotFound(views.html.notFound())) { o =>
        Ok(views.html.report(report, o, signedIn, flagTypes, flagForm))
      }
    }
  }

  def flag(id: String) = Action.async { request =>

    ugcService.report(id).map { r =>

      val boundForm: Form[FlagSubmission] = flagForm.bindFromRequest()(request)
      Logger.info("Bound submission form: " + boundForm)

      boundForm.fold(
        formWithErrors => {
          Logger.info("Form failed to validate: " + formWithErrors)

        },
        submissionDetails => {
          Logger.info("Successfully validated flag submission: " + submissionDetails)
          ugcService.submitFlag(r.id, submissionDetails, request.session.get("token")).map { r =>
            Logger.info("Submitted flag")
          }
        }
      )

      Redirect(routes.ReportController.report(id))
    }

  }


}

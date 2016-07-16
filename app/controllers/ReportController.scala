package controllers

import javax.inject.Inject

import model.User
import model.forms.FlagSubmission
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Request}
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

class ReportController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with WithOwner with I18nSupport{

  def contribution(id: String) = Action.async { implicit request =>

    withOwner { owner =>

      val eventualFlagTypes = ugcService.flagTypes
      val eventualSignedInUser = signedInUserService.signedIn

      for {
        signedIn <- eventualSignedInUser
        contribution <- ugcService.contribution(id, signedIn.map(s => s._2))
        flagTypes <- eventualFlagTypes

      } yield {
        contribution.fold(NotFound(views.html.notFound())) { r =>
          val flagTypeTuples = flagTypes.map(ft => (ft.id, ft.name))
          Ok(views.html.report(r, owner, signedIn.map(s => s._1), flagTypeTuples, flagForm))
        }
      }
    }
  }

  def flag(id: String) = Action.async { implicit request =>

    for {
      signedIn <- signedInUserService.signedIn
      report <- ugcService.contribution(id, signedIn.map(s => s._2))

    } yield {

      report.fold(NotFound(Json.toJson("Not found"))) { r =>

        flagForm.bindFromRequest()(request).fold(
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

        Redirect(routes.ReportController.contribution(id))
      }
    }

  }

  private val flagForm: Form[FlagSubmission] = Form(
    mapping(
      "type" -> optional(text),
      "notes" -> optional(text)
    )(FlagSubmission.apply)(FlagSubmission.unapply)
  )

}

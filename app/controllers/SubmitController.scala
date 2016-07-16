package controllers

import javax.inject.Inject

import model.forms.SubmissionDetails
import model.{Media, User}
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with WithOwner with I18nSupport {

  val submitForm: Form[SubmissionDetails] = Form(
    mapping(
      "headline" -> nonEmptyText,
      "body" -> nonEmptyText
    )(SubmissionDetails.apply)(SubmissionDetails.unapply)
  )

  def prompt() = Action.async { implicit request =>

    val submitPrompt = (owner: User) => {
      for {
        signedIn <- signedInUserService.signedIn

      } yield {
        Ok(views.html.submit(submitForm, owner, signedIn.map(s => s._1)))
      }
    }

    withOwner(submitPrompt)
  }

  def submit() = Action.async(parse.multipartFormData) { implicit request =>

    val submitAction = (owner: User) => {

      signedInUserService.signedIn(request).flatMap { signedIn =>
        // TODO catch not signed in

        submitForm.bindFromRequest().fold(
          formWithErrors => {
            Logger.info("Form failed to validate: " + formWithErrors)
            Future.successful(Ok(views.html.submit(formWithErrors, owner, signedIn.map(s => s._1))))
          },
          submissionDetails => {
            Logger.info("Successfully validated submission details: " + submissionDetails)

            val bearerToken = request.session.get("token").get

            val mediaFile: Option[FilePart[TemporaryFile]] = request.body.file("media")

            val noMedia: Future[Option[Media]] = Future.successful(None)
            val eventualMedia: Future[Option[Media]] = mediaFile.fold(noMedia) { mf =>
              Logger.info("Found media file on request: " + mf)
              ugcService.submitMedia(mf.ref.file, bearerToken)
            }

            eventualMedia.flatMap { media =>
              val submissionResult = ugcService.submit(submissionDetails.headline, submissionDetails.body, media, bearerToken) //TODO should be on the signed in user
              submissionResult.map { or =>
                Logger.info("Submission result: " + or)
                or.fold({
                  Logger.info("Redirecting to homepage")
                  Redirect(routes.Application.index(None, None))
                }
                ) { r =>
                  Logger.info("Redirecting to profile page: " + r.id)
                  Redirect(routes.UserController.profile())
                }
              }
            }
          }
        )
      }
    }

    withOwner(submitAction)
  }

}
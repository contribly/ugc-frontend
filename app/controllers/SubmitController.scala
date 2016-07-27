package controllers

import javax.inject.Inject

import model.{User, Media}
import model.forms.SubmissionDetails
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
    withOwner { owner =>
      for {
        signedIn <- signedInUserService.signedIn

      } yield {
        signedIn.fold {
          Redirect(routes.LoginController.prompt())

        } { s =>
          Ok(views.html.submit(submitForm, owner, Some(s._1)))
        }
      }
    }
  }

  def submit() = Action.async(parse.multipartFormData) { implicit request =>
    withOwner { owner =>

      signedInUserService.signedIn(request).flatMap { signedIn =>

        signedIn.fold {
          Future.successful(Redirect(routes.LoginController.prompt()))
        } { s =>

          submitForm.bindFromRequest().fold(
            formWithErrors => {
              Logger.info("Form failed to validate: " + formWithErrors)
              Future.successful(Ok(views.html.submit(formWithErrors, owner, Some(s._1))))
            },
            submissionDetails => {
              Logger.info("Successfully validated submission details: " + submissionDetails)

              val mediaFile: Option[FilePart[TemporaryFile]] = request.body.file("media")

              val noMedia: Future[Option[Media]] = Future.successful(None)
              val eventualMedia: Future[Option[Media]] = mediaFile.fold(noMedia) { mf =>
                Logger.info("Found media file on request: " + mf)
                ugcService.submitMedia(mf.ref.file, s._2)
              }

              eventualMedia.flatMap { media =>
                val submissionResult = ugcService.submit(submissionDetails.headline, submissionDetails.body, media, s._2) //TODO should be on the signed in user
                submissionResult.map { or =>
                  Logger.info("Submission result: " + or)
                  or.fold({
                    Logger.info("Redirecting to homepage")
                    Redirect(routes.IndexController.index(None, None))
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
    }
  }

}
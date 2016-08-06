package controllers

import javax.inject.Inject

import model._
import model.forms.ContributionForm
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitController @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with WithOwner with I18nSupport {

  val submitForm: Form[ContributionForm] = Form(
    mapping(
      "headline" -> nonEmptyText,
      "body" -> nonEmptyText,
      "location" -> optional(nonEmptyText),
      "latitude" -> optional(bigDecimal),
      "longitude" -> optional(bigDecimal),
      "osmId" -> optional(longNumber()),
      "osmType" -> optional(nonEmptyText)
    )(ContributionForm.apply)(ContributionForm.unapply)
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
          Logger.info("A signed in user is required to submit in this example")
          Future.successful(Redirect(routes.LoginController.prompt()))

        } { s =>

          val signedInUsersApiAccessToken = s._2

          submitForm.bindFromRequest().fold(
            formWithErrors => {
              Logger.info("Form failed to validate: " + formWithErrors)
              Future.successful(Ok(views.html.submit(formWithErrors, owner, Some(s._1))))
            },

            submissionDetails => {
              // The submission past form validation; compose a contribution and submit it to the API
              Logger.info("Successfully validated submission details: " + submissionDetails)

              // If there was a media file on the form submission then we will need to upload it to the media end point before referencing it in our submission
              val mediaFileSeenOnFormSubmission = request.body.file("media")

              val eventualMedia = mediaFileSeenOnFormSubmission.map{ mf =>
                Logger.info("Uploadinf media file on request to the API media end point: " + mf)
                ugcService.submitMedia(mf.ref.file, signedInUsersApiAccessToken)  // The media element is submitted using the signed in user's access token; this ensues the correct ownership
              }.getOrElse(Future.successful(None))

              eventualMedia.flatMap { media =>
                // Compose the contribution and submit it to the contribution API end point including references to any uploaded media elements and location information.
                val contributionSubmission = ContributionSubmission(
                  submissionDetails.headline,
                  Some(submissionDetails.body),
                  media.map(m => Seq(MediaUsage(m, None, Seq()))).getOrElse(Seq()),
                  submissionDetails.location.flatMap { location =>
                    submissionDetails.latitude.flatMap { latitude =>
                      submissionDetails.longitude.map { longitude =>
                        Place(
                          Some(location),
                          Some(LatLong(latitude.toDouble, longitude.toDouble)),
                          submissionDetails.osmId.flatMap( osmId =>
                            submissionDetails.osmType.map { osmType =>
                              Osm(osmId, osmType)
                            }
                          )
                        )
                      }
                    }
                  }
                )

                ugcService.submit(contributionSubmission, signedInUsersApiAccessToken).map { or =>
                  Logger.info("Contribution submission result: " + or)

                  or.fold({
                    Logger.info("Contribution failed; redirecting to homepage")
                    Redirect(routes.IndexController.index(None, None))
                  }
                  ) { r =>
                    Logger.info("Contribution was successful; redirecting to contribution page")
                    // This unmoderated contribution is only visible to the end user. The contribution page will make an authenticiated call to the API
                    Redirect(routes.ContributionController.contribution(r.id))
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
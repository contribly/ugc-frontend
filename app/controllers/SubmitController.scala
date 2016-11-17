package controllers

import javax.inject.Inject

import model._
import model.forms.ContributionForm
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

  val submitForm: Form[ContributionForm] = Form(
    mapping(
      "headline" -> nonEmptyText,
      "body" -> nonEmptyText,
      "location" -> optional(nonEmptyText),
      "latitude" -> optional(bigDecimal),
      "longitude" -> optional(bigDecimal),
      "osmId" -> optional(longNumber()),
      "osmType" -> optional(nonEmptyText),
      "assignment" -> optional(nonEmptyText)
    )(ContributionForm.apply)(ContributionForm.unapply)
  )

  def prompt(assignment: Option[String] = None) = Action.async { implicit request =>
    withOwner { owner =>
      for {
        signedIn <- signedInUserService.signedIn
        openAssignments <- ugcService.assignments(open = Some(true))

      } yield {
        val assignmentOptionsToShow = assignment.flatMap { sa =>
          openAssignments.results.find(a => a.id == sa)
        }.fold(openAssignments.results)(a => Seq(a))

        Ok(views.html.submit(submitForm, owner, signedIn.map(s => s._1), assignmentOptionsToShow))
      }

    }
  }

  def submit() = Action.async(parse.multipartFormData) { implicit request =>
    withOwner { owner =>

      signedInUserService.signedIn(request).flatMap { signedIn =>

        val submissionAccessToken: Future[String] = signedIn.fold {
          Logger.info("No user is signed in. Requesting an anonymous token to submit with")

          ugcService.tokenAnonymous.map { te =>
              te.fold({ l =>
                Logger.error("Failed to obtain anonymous token: " + l)
                throw new RuntimeException(l) // TODO push up to UI
              },{ r =>
                r
            })
          }

        } { s =>
          Logger.info("Using signed in user's access token for submission")
          Future.successful(s._2)
        }

        submissionAccessToken.flatMap { t =>

          ugcService.assignments(open = Some(true)).flatMap { oas =>

            submitForm.bindFromRequest().fold(
              formWithErrors => {
                Logger.info("Form failed to validate: " + formWithErrors)
                Future.successful(Ok(views.html.submit(formWithErrors, owner, signedIn.map(s => s._1), oas.results)))
              },

              submissionDetails => {
                // The submission past form validation; compose a contribution and submit it to the API
                Logger.info("Successfully validated submission details: " + submissionDetails)

                // If there was a media file on the form submission then we will need to upload it to the media end point before referencing it in our submission
                val mediaFileSeenOnFormSubmission: Option[FilePart[TemporaryFile]] = request.body.file("media")

                val eventualMedia = mediaFileSeenOnFormSubmission.map { mf =>
                  Logger.info("Uploading media file on request to the API media end point: " + mf)
                  ugcService.submitMedia(mf.ref.file, t) // The media element is submitted using the same access token as the contribtution; this ensures the correct ownership
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
                            submissionDetails.osmId.flatMap(osmId =>
                              submissionDetails.osmType.map { osmType =>
                                Osm(osmId, osmType)
                              }
                            )
                          )
                        }
                      }
                    },
                    submissionDetails.assignment.flatMap(sa => oas.results.find(a => a.id == sa))
                  )

                  ugcService.submit(contributionSubmission, t).map { or =>
                    Logger.info("Contribution submission result: " + or)

                    or.fold({
                      Logger.info("Contribution failed; redirecting to homepage")
                      Redirect(routes.IndexController.index(None))
                    }
                    ) { r =>
                      Logger.info("Contribution was successful")
                      signedIn.fold {
                        // Anonymous users should not be allowed to preview their unmoderated content.
                        Redirect(routes.IndexController.index(None))
                      } { _ =>
                        // This unmoderated contribution is only visible to the end user. The contribution page will make an authenticiated call to the API
                        Redirect(routes.ContributionController.contribution(r.id))
                      }
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

}
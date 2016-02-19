package controllers

import controllers.Application._
import play.api.mvc.Action
import services.ugc.UGCService

import scala.concurrent.ExecutionContext.Implicits.global

object ReportController {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def report(id: String) = Action.async { request =>
    val eventualReport = ugcService.report(id)
    val eventualOwner = ugcService.owner

    for {
      report <- eventualReport
      owner <- eventualOwner
      signedIn <- signedInUserService.signedIn(request)

    } yield {
      Ok(views.html.report(report, owner, signedIn))
    }
  }

}

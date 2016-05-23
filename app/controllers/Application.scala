
package controllers

import model.User
import play.api.Logger
import play.api.mvc.{Action, Controller, Request, Result}
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller with Pages with WithOwner {

  val ugcService = UGCService
  val signedInUserService = SignedInUserService

  def index(page: Option[Int], hasMediaType: Option[String]) = Action.async { request =>

    val indexPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pagesLinkFor(totalNumber: Long, hasMediaType: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.index(Some(p), hasMediaType).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = hasMediaType)
      val eventualVerifiedSignedInUser = signedInUserService.signedIn(request)

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.index(reports.results, owner, signedIn.map(s => s._1), reports.numberFound, pagesLinkFor(reports.numberFound, hasMediaType)))
      }
    }

    withOwner(request, indexPage)
  }

  def gallery(page: Option[Int]) = Action.async { request =>

    val galleryPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.gallery(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = Some("image"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn(request)

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }

    withOwner(request, galleryPage)
  }

  def videos(page: Option[Int]) = Action.async { request =>

    val videoPage: (Request[Any], User) => Future[Result] = (request: Request[Any], owner: User) => {

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.videos(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = Some("video"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn(request)

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }

    withOwner(request, videoPage)
  }

}
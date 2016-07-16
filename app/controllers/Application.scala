package controllers

import javax.inject.Inject

import model.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def index(page: Option[Int], hasMediaType: Option[String]) = Action.async { implicit request =>

    def indexPage = (owner: User, r: Request[Any]) => {
      def pagesLinkFor(totalNumber: Long, hasMediaType: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.index(Some(p), hasMediaType).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = hasMediaType)
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.index(reports.results, owner, signedIn.map(s => s._1), reports.numberFound, pagesLinkFor(reports.numberFound, hasMediaType)))
      }
    }

    withOwner(indexPage)
  }

  def gallery(page: Option[Int]) = Action.async { implicit request =>

    val galleryPage = (owner: User, r: Request[Any]) => {

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.gallery(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = Some("image"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }

    withOwner(galleryPage)
  }

  def videos(page: Option[Int]) = Action.async { implicit request =>

    val videoPage = (owner: User, r: Request[Any]) => {

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.videos(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, hasMediaType = Some("video"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }

    withOwner(videoPage)
  }

}
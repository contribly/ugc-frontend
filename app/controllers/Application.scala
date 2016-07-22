package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ugc.UGCService
import views.PageLink

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (val ugcService: UGCService, signedInUserService: SignedInUserService, val messagesApi: MessagesApi) extends Controller with Pages with WithOwner with I18nSupport {

  def index(page: Option[Int], mediaTypes: Option[String]) = Action.async { implicit request =>

    withOwner { owner =>

      def pagesLinkFor(totalNumber: Long, mediaTypes: Option[String]): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.index(Some(p), mediaTypes).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, mediaTypes = mediaTypes)
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.index(reports.results, owner, signedIn.map(s => s._1), reports.numberFound, pagesLinkFor(reports.numberFound, mediaTypes)))
      }
    }
  }

  def gallery(page: Option[Int]) = Action.async { implicit request =>

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.gallery(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, mediaTypes = Some("image"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }
  }

  def videos(page: Option[Int]) = Action.async { implicit request =>

    withOwner { owner =>

      def pageLinksFor(totalNumber: Long): Seq[PageLink] = {
        pagesNumbersFor(totalNumber).map(p => PageLink(p, routes.Application.videos(Some(p)).url))
      }

      val eventualReports = ugcService.reports(pageSize = PageSize, page = page, mediaTypes = Some("video"))
      val eventualVerifiedSignedInUser = signedInUserService.signedIn

      for {
        reports <- eventualReports
        signedIn <- eventualVerifiedSignedInUser

      } yield {
        Ok(views.html.gallery(reports.results, owner, signedIn.map(s => s._1), pageLinksFor(reports.numberFound), reports.numberFound))
      }
    }

  }

}
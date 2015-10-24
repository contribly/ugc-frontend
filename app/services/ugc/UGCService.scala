package services.ugc

import model._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.{Logger, Play}

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.concurrent.Future

trait UGCService {

  val apiUrl: String
  val user: String

  val reportsUrl: String = apiUrl + "/reports"
  val noticeboardsUrl: String = apiUrl + "/noticeboards"

  def reports(pageSize: Int, page: Int, tag: Option[String]): Future[SearchResult] = {
    val u = reportsUrl + "?pageSize=" + pageSize + "&page=" + page + tag.fold("")(t => "&tag=" + t)
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[SearchResult]
      }
    }
  }

  def report(id: String): Future[Report] = {
    val u = reportsUrl + "/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Report]
      }
    }
  }
  
  def noticeboards(pageSize: Int, page: Int): Future[NoticeboardSearchResult] = {
    val u = noticeboardsUrl + "?pageSize=" + pageSize + "&page=" + page + "&noticeboardOwnedBy=" + user
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[NoticeboardSearchResult]
      }
    }
  }

  def noticeboard(id: String): Future[Noticeboard] = {
    val u = noticeboardsUrl + "/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Noticeboard]
      }
    }
  }

  def tag(id: String): Future[Tag] = {
    val u = apiUrl + "/tags/" + id
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Tag]
      }
    }
  }

  def tags(): Future[Seq[Tag]] = {
    val u = apiUrl + "/tags"
    Logger.info("Fetching from url: " + u)
    WS.url(u).get.map {
      response => {
        Json.parse(response.body).as[Seq[Tag]]
      }
    }
  }

}

object UGCService extends UGCService {

  override lazy val apiUrl: String = Play.configuration.getString("ugc.api.url").get
  override lazy val user: String = Play.configuration.getString("ugc.user").get

}


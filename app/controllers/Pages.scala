package controllers

import play.api.Logger

trait Pages {

  val PageSize: Int = 40

  def pagesFor(totalNumber: Long): Range = {
    val range = Range(1, (totalNumber.toInt / PageSize) + 2)  // OTODO tests around this rounding
    Logger.info("Page range: " + range)
    range
  }
  
}

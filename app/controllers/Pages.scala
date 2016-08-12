package controllers

trait Pages {

  val PageSize: Int = 20

  def nextPageFor(totalNumber: Long, currentPage: Option[Long]): Option[Long] = {
    val nextPage = currentPage.getOrElse(1L) + 1
    val nextIndex = nextPage * PageSize
    if (nextIndex < totalNumber) {
      Some(nextPage)
    } else {
      None
    }
  }

}

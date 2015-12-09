package controllers

trait Pages {

  val PageSize: Int = 40

  def pagesNumbersFor(totalNumber: Long): Range = {
    // TODO tests around this rounding
    Range(1, (totalNumber.toInt / PageSize) + 2)
  }
  
}

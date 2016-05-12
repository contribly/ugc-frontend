package controllers

trait Pages {

  val PageSize: Int = 40

  def pagesNumbersFor(totalNumber: Long): Range = {
    val i: Int = (totalNumber.toInt - 1) / PageSize
    1 to (i + 1)
  }
  
}

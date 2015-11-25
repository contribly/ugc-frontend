package controllers

trait Pages {

  val PageSize: Int = 40

  def pagesFor(totalNumber: Int): Range = {
    Range(1, totalNumber / PageSize)
  }
  
}

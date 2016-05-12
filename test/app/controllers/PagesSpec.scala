package controllers

import org.specs2.mutable.Specification

class PagesSpec extends Specification with Pages {

  "pagination" should {

    "build page links based on page size and number of available results" in {
      pagesNumbersFor(10) must equalTo((1 to 1))
      pagesNumbersFor(40) must equalTo((1 to 1))
      pagesNumbersFor(79) must equalTo((1 to 2))
      pagesNumbersFor(80) must equalTo((1 to 2))
      pagesNumbersFor(81) must equalTo((1 to 3))
      pagesNumbersFor(110) must equalTo((1 to 3))
    }

  }
}

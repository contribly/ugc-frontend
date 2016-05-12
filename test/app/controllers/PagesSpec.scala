package controllers

import org.specs2.mutable.Specification

class PagesSpec extends Specification with Pages {

  "pagination" should {

    "build page links based on page size and number of available results" in {
      pagesNumbersFor(110) must equalTo((1 to 3))
    }

  }
}

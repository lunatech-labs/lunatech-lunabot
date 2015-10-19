package com.lunatech.lunabot

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class LunabotServletSpec extends ScalatraSpec {
  def is =
    "GET / on LunabotServlet" ^
      "should return status 200" ! root200 ^
      end

  val servlet = new LunabotServlet(Map(1 -> "token"))
  addServlet(servlet, "/*")

  def root200 = get("/") {
    //page exists
    status must_== 200
  }

}

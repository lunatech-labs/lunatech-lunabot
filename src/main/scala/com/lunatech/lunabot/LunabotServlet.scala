package com.lunatech.lunabot

import com.lunatech.lunabot.model._
import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import org.scalatra.json._
import scala.sys.process._
import dispatch._, Defaults._
import scala.util.{Failure, Success}


/**
 * Application to handle POST requests received from HipChat.
 *
 * A HipChat request contains information in JSON format which includes
 * a piece of code to be executed in scala REPL in asynchronous mode.
 * Once the response is sent by REPL, an POST request including the
 * scala result is sent to HipChat.
 */
class LunabotServlet extends ScalatraServlet with JacksonJsonSupport {

  val logger = LoggerFactory.getLogger(getClass)

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  before() {
    contentType = formats("json")
  }

  get("/") {
    "Hello Lunabot!"
  }

  post("/repl") {
    logger.debug(request.body)
    val jsonValue = parse(request.body.replace("mention_name", "mentionName"))
    val hipchatMsg = jsonValue.extract[HipChatMessage]
    val authToken = System.getenv("AUTH_TOKEN")
    val urlStr = "https://api.hipchat.com/v2/room/roomId/notification?auth_token="

    val executionFuture: Future[String] = Future {
      val cmd1: String = hipchatMsg.item.message.message.replace("/scala ", "")
      REPLproc.run(cmd1)
    }

    executionFuture.onComplete {
      case Success(result) => {
        //Send response to HipChat Room
        val myRequest = sendAsyncResponse(urlStr, hipchatMsg, authToken) <<
          """{"color": "green", "message": """ + "\"" + "@" + hipchatMsg.item.message.from.mentionName + " " +
            result.replace("\n", "\\n") + "\"" + """, "notify": false, "message_format": "text"}"""
        dispatch.Http(myRequest)
      }
      case Failure(ex: Exception) => {
        val myRequest = sendAsyncResponse(urlStr, hipchatMsg, authToken) <<
          s"""{"color": "red", "message": "Sorry your scala code has failed with ${ex.getMessage}
             |", "notify": false, "message_format": "text"}""".stripMargin
        dispatch.Http(myRequest)
      }

    }

  }

  def sendAsyncResponse(urlStr: String, hipchatMsg: HipChatMessage, authToken: String): Req = {
    val roomId = hipchatMsg.item.room.id
    val updatedUrl = urlStr.replace("roomId", roomId.toString).concat(authToken)
    val request = dispatch.url(updatedUrl).setContentType("application/json", "UTF-8").POST
    request
  }
}


package com.lunatech.lunabot

import com.lunatech.lunabot.model._
import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
// JSON handling support from Scalatra
import org.scalatra.json._
import dispatch._, Defaults._
import scala.util.{Failure, Success}
// JSON-related libraries
import org.json4s._
import org.json4s.JsonDSL._
import com.typesafe.config.ConfigFactory
import java.io.File

/**
 * Application to handle POST requests received from HipChat.
 *
 * A HipChat request contains information in JSON format which includes
 * a piece of code to be executed in scala REPL in asynchronous mode.
 * Once the response is sent by REPL, an POST request including the
 * scala result is sent to HipChat.
 */
class LunabotServlet extends ScalatraServlet with JacksonJsonSupport {

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  val logger = LoggerFactory.getLogger(getClass)
  lazy val lunabotSettingsFilepath = System.getenv("lunabot_settings_filepath")
  lazy val configFactory = ConfigFactory.parseFile(new File(lunabotSettingsFilepath))


  // Before every action runs, set the content type to be in JSON format.
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
    val cmd: String = hipchatMsg.item.message.message.replace("/scala ", "")

    //Send response to HipChat Room.
    executeExpression(cmd).onComplete {
      case Success(result) =>
        val jsonResponse: String = compact(render(("color" -> "green") ~
          ("message" -> result) ~
          ("notify" -> false) ~
          ("message_format" -> "text")))
        val myRequest: Req = prepareResponse(hipchatMsg) << jsonResponse
        dispatch.Http(myRequest)

      case Failure(ex: Exception) =>
        val errorResp = compact(render(("color" -> "red") ~
          ("message" -> s"Sorry your scala code has failed with ${ex.getMessage}") ~
          ("notify" -> false) ~
          ("message_format" -> "text")))
        val myRequest = prepareResponse(hipchatMsg) << errorResp
        dispatch.Http(myRequest)
    }
  }

  /*
  * Form the url where response should be sent by filling the right roomId and corresponding authToken from configuration
  * @param hipchatMsg The HipChatMessage object received by the post request to Lunabot
  * @return The request that is about to be sent with the right destination url
  *  */
  def prepareResponse(hipchatMsg: HipChatMessage): Req = {
    val roomId = hipchatMsg.item.room.id
    val authToken = configFactory.getString(s"AUTH_TOKENS.$roomId.token")
    val urlStr = s"https://api.hipchat.com/v2/room/$roomId/notification?auth_token=$authToken"
    val myRequest = dispatch.url(urlStr).setContentType("application/json", "UTF-8").POST
    myRequest
  }

  /*
  * Execute the expression included in the post request and get a Future string with the response
  * @param scalaExpr The expression that is to be evaluated by the repl.
  * @return A Future String containing the response from the repl.
  * */
  def executeExpression(scalaExpr: String): Future[String] = Future {
    REPLproc.run(scalaExpr)
  }

}

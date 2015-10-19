package com.lunatech.lunabot

import java.net.URL

import com.lunatech.lunabot.model._
import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import org.scalatra.json._
import dispatch._, Defaults._
import scala.util.{Failure, Success, Try}
import org.json4s._
import org.json4s.JsonDSL._

/**
 * Application to handle POST requests received from HipChat.
 *
 * A HipChat request contains information in JSON format which includes
 * a piece of code to be executed in scala REPL in asynchronous mode.
 * Once the response is sent by REPL, an POST request including the
 * scala result is sent to HipChat.
 */
class LunabotServlet(val tokens: Map[Int, String]) extends ScalatraServlet with JacksonJsonSupport {

  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats

  val logger = LoggerFactory.getLogger(getClass)
  private val COLOR_PARAM: String = "color"
  private val MESSAGE_PARAM: String = "message"
  private val MESSAGE_FORMAT: String =  "message_format"
  private val NOTIFY_PARAM: String = "notify"

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  get("/") {
    "Hello Lunabot!"
  }

  post("/repl") {
    logger.debug(request.body)

    val jsonValue: JValue = parse(request.body.replace("mention_name", "mentionName"))
    val hipChatMessage: HipChatMessage = jsonValue.extract[HipChatMessage]
    val cmd: String = hipChatMessage.item.message.message.replace("/scala ", "")

    lazy val futureResponse: Try[JValue] = evaluateExpression(hipChatMessage.item.message.message)
    val maybeResponse: Option[URL] = responseUrl(hipChatMessage.item.room.id)

    val maybeResult: Either[String, (URL, JValue)] = maybeResponse match {
      case Some(url) =>
        futureResponse match {
          case Success(value) => Right(url, value)
          case Failure(ex) =>
            Right(url, responseJValue(s"Sorry your scala code has failed with ${ex.getMessage}", "red"))
        }
      case None =>
        val message: String = s"Unable to find the roken for the room ${hipChatMessage.item.room.id}"
        logger.warn(message)
        Left(message)

    }

    maybeResult match {
      case Right((url, jvalue)) => dispatch.Http(constructRequest(url.toString()) << compact(jvalue))
      case Left(message) => logger.error(message)
    }

  }

  /*
  * Form the url where response should be sent by filling the right roomId and corresponding authToken from configuration
  * @param hipchatMsg The HipChatMessage object received by the post request to Lunabot
  * @return The request that is about to be sent with the right destination url
  *  */
  def constructRequest(url: String): Req = {
    dispatch.url(url).setContentType("application/json", "UTF-8").POST
  }
  
  /*
   * Creates the url where Lunabot sends its POST request
   * @param hipchatMsg The message that is included in the post request received by Lunabot
   */
  private def responseUrl(roomId: Int): Option[URL] = tokens.get(roomId).map {
    t => new URL(s"https://api.hipchat.com/v2/room/$roomId/notification?auth_token=$t")
  }

  private def evaluateExpression(expression: String): Try[JValue] = REPLproc.run(expression).map {
    responseJValue(_, "green")
  }

  def responseJValue(message: String, color: String, notify: Boolean = false, messageFormat: String = "text"): JValue =
    render((COLOR_PARAM -> color) ~
      (MESSAGE_PARAM -> message) ~
      (NOTIFY_PARAM -> false) ~
      (MESSAGE_FORMAT -> messageFormat))

}


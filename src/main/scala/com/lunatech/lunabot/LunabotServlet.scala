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
 * @constructor create a new Servlet with a Map of roomID->token
 * @param tokens map with all roomID->token available
 */
class LunabotServlet(val tokens: Map[Int, String]) extends ScalatraServlet with JacksonJsonSupport {

  val logger = LoggerFactory.getLogger(getClass)
  // Sets up automatic case class to JSON output serialization, required by
  // the JValueResult trait.
  protected implicit val jsonFormats: Formats = DefaultFormats
  // Default parameters for a POST request to HipChat
  private val COLOR_PARAM: String = "color"
  private val MESSAGE_PARAM: String = "message"
  private val MESSAGE_FORMAT: String = "message_format"
  private val NOTIFY_PARAM: String = "notify"

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  get("/") {
    "Hello Lunabot!"
  }

  /*
   * A HipChat POST request contains information in JSON format which includes
   * an expression to be executed in scala REPL in asynchronous mode.
   * Once the response is sent by REPL, an POST request with the scala result is sent to HipChat.
   */
  post("/repl") {
    logger.debug(request.body)

    // Parse JSON value from HipChat POST request
    val jsonValue: JValue = parse(request.body.replace("mention_name", "mentionName"))
    val hipChatMessage: HipChatMessage = jsonValue.extract[HipChatMessage]

    // Expression to send to the REPL
    val cmd: String = hipChatMessage.item.message.message.replace("/scala ", "")
    logger.debug(s"Processing $cmd")

    // Response from the REPL
    lazy val triedResponse: Try[JValue] = evaluateExpression(cmd)

    // Prepare POST request
    val maybeResponse: Option[URL] = responseUrl(hipChatMessage.item.room.id)
    val maybeResult: Either[String, (URL, JValue)] = maybeResponse match {
      case Some(url) =>
        triedResponse match {
          case Success(value) => Right(url, value)
          case Failure(ex) =>
            Right(url, responseJValue(s"Sorry your scala code has failed with ${ex.getMessage}", "red"))
        }
      case None =>
        val message: String = s"Unable to find the token for the room ${hipChatMessage.item.room.id}"
        Left(message)
    }

    // Send POST request to HipChat if possible, log error otherwise
    maybeResult match {
      case Right((url, jvalue)) => dispatch.Http(constructRequest(url.toString) << compact(jvalue))
      case Left(message) => logger.error(message)
    }
  }

  /**
   * Constructs the POST request header for the given URL
   *
   * @param url String containing URL used to send the POST request
   * @return Req header
   */
  private def constructRequest(url: String): Req = {
    dispatch.url(url).setContentType("application/json", "UTF-8").POST
  }

  /**
   * Prepares a URL based on the room ID
   *
   * @param roomId room id where to send the POST request
   * @return Some(URL) if room id exists, None otherwise
   */
  private def responseUrl(roomId: Int): Option[URL] = tokens.get(roomId).map {
    t => new URL(s"https://api.hipchat.com/v2/room/$roomId/notification?auth_token=$t")
  }

  /**
   * Makes a REPL call to execute on expression and prepares the POST request's body
   *
   * @param expression Scala expression to be executed in the REPL
   * @return POST request's body in JSON format
   */
  private def evaluateExpression(expression: String): Try[JValue] = ReplProc.run(expression).map {
    responseJValue(_, "green")
  }

  /**
   * Prepares JSON value with the format required by HipChat
   *
   * @param message Result of the expression executed in the REPL
   * @param color HipChat color for the message
   * @param notify True to notify HipChat users, false otherwise
   * @param messageFormat Message format to be displayed on HipChat
   * @return JSON value having HipChat required format
   */
  private def responseJValue(message: String, color: String, notify: Boolean = false, messageFormat: String = "text"): JValue =
    render((COLOR_PARAM -> color) ~
      (MESSAGE_PARAM -> message) ~
      (NOTIFY_PARAM -> false) ~
      (MESSAGE_FORMAT -> messageFormat))

}

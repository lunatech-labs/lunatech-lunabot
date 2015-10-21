import java.io.File

import com.lunatech.lunabot._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatra._
import javax.servlet.ServletContext
import scala.collection.JavaConverters._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    // Get the tokens from the env variable.
    val lunabotSettingsFilepath = System.getenv("lunabot_settings_filepath")
    val configFactory: Config = ConfigFactory.parseFile(new File(lunabotSettingsFilepath))
    val authTokens: Seq[Config] = configFactory.getConfigList("AUTH_TOKENS").asScala.toSeq

    val tokens: Map[Int, String] = authTokens.map {
      at => at.getInt("roomId") -> at.getString("token")
    }.toMap

    context.mount(new LunabotServlet(tokens), "/*")
  }
}

import java.io.File

import com.lunatech.lunabot._
import com.typesafe.config.ConfigFactory
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {

    val lunabotSettingsFilepath = System.getenv("lunabot_settings_filepath")
    val configFactory = ConfigFactory.parseFile(new File(lunabotSettingsFilepath))

    val tokens  = Map(1 -> "token1")

    context.mount(new LunabotServlet(tokens), "/*")
  }
}

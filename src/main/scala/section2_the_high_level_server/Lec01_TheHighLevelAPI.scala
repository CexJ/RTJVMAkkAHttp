package section2_the_high_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object Lec01_TheHighLevelAPI {

  implicit val system = ActorSystem("Lec01_TheHighLevelAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") { // directive
      complete(StatusCodes.OK) // directive
    }

  Http().bindAndHandle(simpleRoute, "localhost", 8080)

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }

    }

  // chaining directives

  val chainedRoute: Route =
    path("myEndPoint") {
      get {
        complete(StatusCodes.OK)
      } ~ // VERY IMPORTANT (no compiler or runtime error but doesn't work!!!)
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("home") {
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          | <body>
          |   Hello akka http!
          | </body>
          |</html>
          |""".stripMargin
      ))
    }

}

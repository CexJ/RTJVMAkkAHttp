package section2_the_high_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer

object Lec05_HandlingExceptions extends App {

  implicit val system = ActorSystem("Lec05_HandlingExceptions")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val simpleRoute = {
    path("api" / "people"){
      get {
        throw new RuntimeException("too much")
      } ~ post {
        parameter("id") { id =>
          if(id.length >= 2) throw new NoSuchElementException("param id cannot be found")
          else complete(StatusCodes.OK)
        }
      }
    }
  }

  // the default handler add Http 500 status

  implicit val exceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }


}

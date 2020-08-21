package section2_the_high_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, RejectionHandler}
import akka.stream.ActorMaterializer

object Lec04_HandlingRejections extends App {

  implicit val system = ActorSystem("Lec04_HandlingRejections")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val simpleRoute =
    path("api" / "myendpoint"){
      get{
        complete(StatusCodes.OK)
      } ~
      parameter("id") { _ =>
        complete(StatusCodes.OK)
      }
    }

  /**
   * rejection handler
   */
  val badRequestHandler: RejectionHandler = { rejections =>
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenHandler: RejectionHandler = { rejections =>
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandler =
    handleRejections(badRequestHandler) {
      path("api" / "myendpoint"){
        get{
          complete(StatusCodes.OK)
        } ~
        post {
          handleRejections(forbiddenHandler) {
            parameter("id") { _ =>
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }

  implicit val customRejectionHandler =
    RejectionHandler.newBuilder()
    .handle{
      case m: MethodRejection => complete("rejected method")
    }.handle{
      case m: MissingQueryParamRejection => complete("no param")
    }.result() // it is implicitly passed
}

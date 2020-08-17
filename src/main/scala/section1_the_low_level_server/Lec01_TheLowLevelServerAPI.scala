package section1_the_low_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Lec01_TheLowLevelServerAPI extends App {

  implicit val system = ActorSystem("Lec01_TheLowLevelServerAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val serverSource = Http().bind("localhost",8000)
  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Excepted incoming connection from ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource.to(connectionSink).run()

  serverBindingFuture.onComplete {
    case Success(binding) => println("Server binding successful")
    case Failure(exception) => println(s"Server binding failure cause: $exception")
  }

  /**
   * Synchronously
   */

  val requestHandler: Function[HttpRequest, HttpResponse] = {
    case HttpRequest(HttpMethods.GET,Uri.Path("/home"),_,_,_) => // method, URI, HTTP header, content, protocol
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka Http!
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Not found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection]{ connnection =>
    connnection.handleWithSyncHandler(requestHandler)
  }

  Http().bind("localhost", 8080).runWith(httpSyncConnectionHandler)
  // alternatively
  Http().bindAndHandleSync(requestHandler, "localhost", 8081)

  /**
   * Async
   */

  val asyncRequestHandler: Function[HttpRequest, Future[HttpResponse]] = {
    case HttpRequest(HttpMethods.GET,Uri.Path("/home"),_,_,_) =>
      Future { HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka Http!
            | </body>
            |</html>
            |""".stripMargin
        )
      )}

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future{ HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Not found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )}
  }

  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection]{ connnection =>
    connnection.handleWithAsyncHandler(asyncRequestHandler)
  }

  Http().bind("localhost", 8082).runWith(httpAsyncConnectionHandler)
  // alternatively
  Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 8083)


  /**
   * Async via Flow
   */

  val flowRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET,Uri.Path("/home"),_,_,_) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka Http!
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Not found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  Http().bind("localhost", 8084).runForeach { connection =>
    connection.handleWith(flowRequestHandler)
  }
  // alternatively
  val bindingFuture = Http().bindAndHandle(flowRequestHandler, "localhost", 8085)

  // shutdown server
  bindingFuture.flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}

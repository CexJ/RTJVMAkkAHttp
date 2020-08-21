package section2_the_high_level_server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer

object Lec02_Directives extends App {

  implicit val system = ActorSystem("Lec02_Directives")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._


  /**
   * Filtering
   */

  val simpleHttpMethodRoute =
    get { // put, post, patch, delete, head, options
      complete(StatusCodes.OK)
    }

  val simplePathRoute =
    path("about") {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |Hello
          |""".stripMargin
      ))
    }

  val complexPathRoute =
    path("api" / "endpoint") {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |Hello
          |""".stripMargin
      ))
    }

  val dontConfuseComplexPathRoute =
    path("api/endpoint") { // "/" => %2f urlEncoded
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
        """
          |Hello
          |""".stripMargin
      ))
    }

  val pathEndRoute =
    pathEndOrSingleSlash{ // localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    }

  /**
   * Extraction
   */

  // GET on api/item/42

  val valuePathRoute =
    path("api" / "item" / IntNumber) { value: Int =>
      complete(StatusCodes.OK)
    }

  val multiCaluePathRoute =
    path("api" / "order" / IntNumber / IntNumber) { (value1, value2)  =>
      complete(StatusCodes.OK)
    }

  // GET on api/item?id=42
  val queryParamRoute =
    path("api" / "item" ) {
      parameter("id"){ id: String =>
        complete(StatusCodes.OK)
      }
    }

  val queryParamCastRoute =
    path("api" / "item" ) {
      parameter("id".as[Int]){ id: Int =>
        complete(StatusCodes.OK)
      }
    }

  val optQueryParamRoute =
    path("api" / "item" ) {
      parameter('id.as[Int]){ id: Int =>
        complete(StatusCodes.OK)
      }
    }

  val requestRoute =
    path("api" / "item" ) {
      extractRequest { httpRequest: HttpRequest => // request...
        complete(StatusCodes.OK)
      }
    }

  /**
   * composite
   */

  val nestedRoute =
    path("api" / "item"){
      get {
        complete(StatusCodes.OK)
      }
    }

  val compactPathRoute = (path("api" / "item") & get){
    complete(StatusCodes.OK)
  }

  val compactExtractRoute = (path("control") & extractRequest & extractLog){ (httpRequest, log) =>
    complete(StatusCodes.OK)
  }

  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~
    path("aboutUs") {
      complete(StatusCodes.OK)
    }

  val notRepeatedRoute =
    (path("about") | path("aboutUs")) {
      complete(StatusCodes.OK)
    }

  val pathRoute =
    path(IntNumber) { value =>
      complete(StatusCodes.OK)
    }

  val paramRoute =
    parameter('id.as[Int]) { value =>
      complete(StatusCodes.OK)
    }

  val pathOrParamRoute =
    (path(IntNumber) | parameter('id.as[Int])) { value =>
      complete(StatusCodes.OK)
    }

  /**
   * actionable
   */

  val completeRoute =
    complete(StatusCodes.OK) // Http 200

  val failRoute =
    failWith(new RuntimeException) // Http 500

  val rejectRoute =
    reject // move to the next accepting directive
}

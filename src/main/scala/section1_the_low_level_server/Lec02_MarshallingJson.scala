package section1_the_low_level_server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.stream.ActorMaterializer
import section1_the_low_level_server.GuitarDB.{CreateGuitar, FindAllGuitars, FindGuitar, GuitarCreated}
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

case class Guitar(make: String, model: String)

object GuitarDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars
}


class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId = 0

  override def receive: Receive = {
    case CreateGuitar(guitar) =>
      log.info(s"Adding $guitar with $currentGuitarId")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
    case FindAllGuitars =>
      log.info("Searching for all guitars")
      sender() ! guitars.values.toList
    case FindGuitar(id) =>
      log.info(s"Searching for guitar $id")
      sender() ! guitars.get(id)
  }
}

trait GuitarStoreGuitarProtocol extends DefaultJsonProtocol {

  implicit val guitarFormat = jsonFormat2(Guitar)
}

object Lec02_MarshallingJson extends App with GuitarStoreGuitarProtocol {

  implicit val system = ActorSystem("Lec02_MarshallingJson")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


  /**
   * GET on localhost:8080/api/guitar => ALL the guitars
   * GET on localhost:8080/api/guitar?id=X => the guitar X
   * POST on localhost:8080/api/guitar => add the guitar
   */

  val guitar = Guitar("fender","stratocaster")
  println(guitar.toJson.prettyPrint)

  val json =
    """
      |{
      | "make": "fender",
      | "model": "stratocaster"
      |}
      |""".stripMargin

  println(json.parseJson.convertTo[Guitar])

  val guitarDB = system.actorOf(Props[GuitarDB], "guitarDB")

  val guitarList = List(
    Guitar("fender", "stratocaster"),
    Guitar("gibson", "les paul"),
    Guitar("martin", "LX1")
  )

  guitarList.foreach(guitarDB ! _)

  import scala.concurrent.duration._
  implicit val timeout = Timeout(2 seconds)

  def getGuitar(query: Uri.Query): Future[HttpResponse] = {
    query.get("id").map(_.toInt) match {
      case None => Future{HttpResponse(StatusCodes.NotFound)}
      case Some(id) => (guitarDB ? FindGuitar(id)).mapTo[Option[Guitar]].map {
        case None => HttpResponse(StatusCodes.NotFound)
        case Some(guitar) =>
        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guitar.toJson.prettyPrint)
        )
      }
    }
  }

  /**
   * server code
   */
  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"),_,_,_) =>
      val query = uri.query()
      if(query.isEmpty){
        val guitarsFuture: Future[List[Guitar]] = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]
        guitarsFuture.map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }
      } else {
        getGuitar(query)
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"),_,entity,_) =>
      // entity is a source of byteString
      val strictEntity = entity.toStrict(3 seconds)
      strictEntity.flatMap{ se =>
        (guitarDB ?
          CreateGuitar(se.data.utf8String.parseJson.convertTo[Guitar]))
          .mapTo[GuitarCreated]
          .map { _ => HttpResponse(StatusCodes.OK)}
      }
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
}

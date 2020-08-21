package section2_the_high_level_server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import section2_the_high_level_server.GameAreaMap.{AddPlayer, GetAllPlayers, GetPlayersByClass}
import akka.pattern.ask
import akka.util.Timeout
import spray.json._


case class Player(nickname: String, characterClass: String, level: Int)

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerFormat = jsonFormat3(Player)
}

object GameAreaMap{
  case object GetAllPlayers
  case class GetPlayer(nickname: String)
  case class GetPlayersByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccess
}
class GameAreaMap extends Actor with ActorLogging {

  import GameAreaMap._

  var players: Map[String, Player] = Map()

  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("GetAllPlayers")
      sender() ! players
    case GetPlayer(nickname) =>
      log.info(s"GetPlayer($nickname)")
      sender() ! players.get(nickname)
    case GetPlayersByClass(characterClass) =>
      log.info(s"GetPlayersByClass($characterClass)")
      sender() ! players.values.filter(_.characterClass == characterClass).toList
    case AddPlayer(player) =>
      log.info(s"AddPlayer($player)")
      players = players + (player.nickname -> player)
      sender() ! OperationSuccess
    case RemovePlayer(player) =>
      log.info(s"RemovePlayer($player)")
      players = players - player.nickname
      sender() ! OperationSuccess
  }
}

object Lec03_MarshallingJSON extends App
  with PlayerJsonProtocol
  with SprayJsonSupport {

  implicit val system = ActorSystem("Lec03_MarshallingJSON")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._
  import GameAreaMap._

  val rtjvmGameMap = system.actorOf(Props[GameAreaMap])
  val playerList = List(
    Player("martin","warior", 70),
    Player("roland","elf", 70),
    Player("daniel","wizard", 30)
  )

  playerList.foreach(rtjvmGameMap ! AddPlayer(_))

  /**
   * GET /api/player returns all the players
   * GET /api/player/(nickname) returns the player
   * GET /api/player?nickname=X returns the player
   * GET /api/player/class/(charClass) get player by charClass
   * POST /api/player add player
   * DELETE /api/player delete player
   */

  import scala.concurrent.duration._
  implicit val timeout = Timeout(2 seconds)
  val rtjvmGameRoute =
    pathPrefix("api" / "player"){
      get {
        path("class" / Segment) { charClass =>
          val playersByChar = (rtjvmGameMap ? GetPlayersByClass(charClass)).mapTo[List[Player]]
          complete(playersByChar)
        } ~
        (path(Segment) | parameter('nickname)) { nickname =>
          val player = (rtjvmGameMap ? GetPlayer(nickname)).mapTo[Option[Player]]
          complete(player)
        } ~
        pathEndOrSingleSlash{
          val players = (rtjvmGameMap ? GetAllPlayers).mapTo[List[Player]]
          complete(players)
        }
      } ~
      post {
        entity(as[Player]){ player =>
          complete((rtjvmGameMap ? AddPlayer(player)).map(_ => StatusCodes.OK))
        }
      } ~
      delete {
        // TODO
        reject
      }
    }
}

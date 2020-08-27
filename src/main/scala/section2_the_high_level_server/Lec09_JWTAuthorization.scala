package section2_the_high_level_server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import section2_the_high_level_server.SecurityDomain.LoginRequest
import spray.json._

import scala.util.{Failure, Success}

object SecurityDomain extends DefaultJsonProtocol {
  case class LoginRequest(username: String, password: String)

  implicit val loginRequestFormat = jsonFormat2(LoginRequest)

}

object Lec09_JWTAuthorization extends App with SprayJsonSupport {

  implicit val system = ActorSystem("Lec09_JWTAuthorization")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val superSecretPasswordDB = Map(
    "admin" -> "admin",
    "daniel" -> "rtjvm"
  )

  val algorithm = JwtAlgorithm.HS256
  val key = "rtjvm-secret"

  def checkPassword(username: String, password: String): Boolean =
    superSecretPasswordDB.contains(username) && superSecretPasswordDB(username) == password

  def createToken(username: String, expirationPeriodInDays: Int): String = {
    val claims = JwtClaim(
      expiration = Some(System.currentTimeMillis()/1000 + TimeUnit.DAYS.toSeconds(expirationPeriodInDays)),
      issuedAt = Some(System.currentTimeMillis()/1000),
      issuer = Some("rtjvm.com")
      )
    JwtSprayJson.encode(claims,key,algorithm)
  }



  def isTokenExpired(token: String): Boolean = JwtSprayJson.decode(token, key, Seq(algorithm)) match {
    case Success(claims) => claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
    case Failure(_) => true
  }

  def isTokenValid(token: String): Boolean = JwtSprayJson.isValid(token, key, Seq(algorithm))

  val loginRoute =
  post {
    entity(as[LoginRequest]) {
      case LoginRequest(username, password) if checkPassword(username,password) =>
        val token = createToken(username, 1)
        respondWithHeader(RawHeader("Access-Token", token)){
          complete(StatusCodes.OK)
        }
      case _ =>
        complete(StatusCodes.Unauthorized)
    }
  }


  val authenticatedRoute =
    (path("secureendpoint") & get){
      optionalHeaderValueByName("Authorisation") {
        case Some(token) if isTokenExpired(token) =>
          complete(HttpResponse(
            StatusCodes.Unauthorized,
            entity = "Token expired"
          ))
        case Some(token) if isTokenValid(token) =>
          complete("User accessed authorized endpoint!")
        case _ =>
          complete(HttpResponse(
            StatusCodes.Unauthorized,
            entity = "Token is invalid"))
      }
    }

  val route = loginRoute ~ authenticatedRoute

  Http().bindAndHandle(route,"localhost", 8080)
}
